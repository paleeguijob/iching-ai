package com.iching.ai.session

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.SignInProvider
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.platform.NativeAuth
import com.iching.ai.platform.NativeBridges
import com.iching.ai.platform.NativeException
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Behaves like Firebase Auth: the signed-in user changes when linking finds an existing account. */
private class SwitchingAuth : NativeAuth {
    var uid: String? = "anon"
    var provider = "anonymous"
    var email: String? = null
    /** The uid Google answers with; the current one means a plain link, another means that account already exists. */
    var googleAnswer: Pair<String?, String?> = "anon" to null
    var nextAnonymousUid: String? = "fresh"

    override fun currentUserId() = uid
    override fun currentProvider() = provider
    override fun currentEmail() = email
    override fun signInAnonymously(callback: ResultCallback) {
        uid = nextAnonymousUid
        provider = "anonymous"
        callback.onResult(uid, if (uid == null) "offline" else null)
    }
    override fun idToken(forceRefresh: Boolean, callback: ResultCallback) = callback.onResult("token-$uid", null)
    override fun appCheckToken(callback: ResultCallback) = callback.onResult("ac", null)
    override fun linkApple(callback: ResultCallback) = callback.onResult(null, "unsupported")
    override fun linkGoogle(callback: ResultCallback) {
        val (value, error) = googleAnswer
        if (value != null) {
            uid = value
            provider = "google.com"
            email = "seeker@example.com"
        }
        callback.onResult(value, error)
    }
    override fun supportsApple() = false
    override fun supportsGoogle() = true
    override fun signOut() {
        uid = null
        email = null
    }
    override fun deviceId() = "device-123456"
    override fun appVersion() = "1.1"
}

private val monthly = StoreProduct("m", "iching_premium_monthly", ProductKind.MONTHLY, "", "฿49", "เดือน", null)
private val coinPack = StoreProduct("c", "iching_coins_10", ProductKind.COINS, "", "฿29", null, null)
private val premium = AccountStatus.UNKNOWN_FREE.copy(premium = true)

class AccountAndMoneyTest {
    private val auth = SwitchingAuth()
    private val purchases = FakePurchases()
    private val api = FakeApi()
    private val journal = FakeJournal()

    private fun session(scope: CoroutineScope) = AppSession(
        NativeBridges(auth, purchases, NoAds, NoAnalytics, NoNotifications), FirebaseApiSession(auth), api, journal, NoSettings, scope,
    )

    private fun TestScope.started(): AppSession = session(backgroundScope).also {
        it.start()
        testScheduler.runCurrent()
    }

    // ---------- connecting an account ----------

    @Test
    fun linkingKeepsTheSameCustomerSoPurchasesStayPut() = runTest {
        val session = started()

        val outcome = session.link(SignInProvider.GOOGLE).getOrThrow()
        val account = outcome.account

        assertTrue(outcome.carriedOver)
        assertEquals(emptyList(), api.merged, "nothing was left behind")
        assertEquals("anon", account.uid)
        assertEquals(SignInProvider.GOOGLE, account.provider)
        assertEquals("seeker@example.com", account.email)
        assertEquals(listOf("anon"), purchases.logIns)
        assertEquals(account, session.account.value)
    }

    @Test
    fun linkingShowsTheAccountsStatusRightAway() = runTest {
        val session = started()
        api.statusNow = premium

        session.link(SignInProvider.GOOGLE)

        assertTrue(session.status.value.premium)
    }

    @Test
    fun aFailedLinkLeavesTheSeekerWhereTheyWere() = runTest {
        val session = started()
        auth.googleAnswer = null to "network"

        assertTrue(session.link(SignInProvider.GOOGLE).isFailure)

        assertEquals("anon", session.account.value?.uid)
        assertEquals(SignInProvider.ANONYMOUS, session.account.value?.provider)
        assertEquals(listOf("anon"), purchases.logIns)
    }

    @Test
    fun cancellingTheSheetIsReportedAsCancelled() = runTest {
        val session = started()
        auth.googleAnswer = null to "cancelled"

        val failure = session.link(SignInProvider.GOOGLE).exceptionOrNull()

        assertIs<NativeException>(failure)
        assertEquals("cancelled", failure.message)
    }

    @Test
    fun switchingToAnExistingAccountMovesTheCustomerToo() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null
        api.statusNow = premium

        val account = session.link(SignInProvider.GOOGLE).getOrThrow().account

        assertEquals("existing", account.uid)
        assertEquals(listOf("anon", "existing"), purchases.logIns)
        assertTrue(session.status.value.premium)

        api.statusAfterSync = premium
        session.purchase(monthly)
        assertEquals(setOf<String?>("existing"), purchases.purchasesUnder.toSet())
    }

    @Test
    fun aPurchaseAfterSwitchingNeverLandsOnTheOldAccountEvenIfLoginFailedThen() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null
        purchases.logInWorks = false
        assertTrue(session.link(SignInProvider.GOOGLE).isSuccess)

        assertIs<PurchaseResult.Error>(session.purchase(monthly))
        assertEquals(emptyList(), purchases.purchasesUnder)

        purchases.logInWorks = true
        api.statusAfterSync = premium
        assertIs<PurchaseResult.Success>(session.purchase(monthly))
        assertEquals(listOf<String?>("existing"), purchases.purchasesUnder)
    }

    /** Someone subscribed on a new phone before signing in: the subscription sits on the anonymous id. */
    @Test
    fun switchingToAnExistingAccountBringsAlongWhatWasBoughtBeforeSigningIn() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null

        session.link(SignInProvider.GOOGLE)

        assertEquals(1, purchases.restores, "the store purchase should move to the account signed into")
        assertTrue(api.syncCalls > 0, "the server should be told straight away")
    }

    @Test
    fun switchingToAnExistingAccountMovesCoinsAndReadingsFromTheOneLeft() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 10)

        val outcome = session.link(SignInProvider.GOOGLE).getOrThrow()

        assertEquals(listOf("token-anon"), api.merged, "with the token of the account left, not the one signed into")
        assertTrue(outcome.carriedOver)
    }

    @Test
    fun nothingIsRestoredUnderTheOldCustomerWhenLoginFailsAfterSwitching() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null
        purchases.logInWorks = false

        val outcome = session.link(SignInProvider.GOOGLE).getOrThrow()

        assertEquals(0, purchases.restores)
        assertFalse(outcome.carriedOver)
    }

    @Test
    fun aMergeThatFailedIsReportedAndRetriedByTheNextRestore() = runTest {
        val session = started()
        auth.googleAnswer = "existing" to null
        api.mergeFails = true

        assertFalse(session.link(SignInProvider.GOOGLE).getOrThrow().carriedOver)
        assertEquals(emptyList(), api.merged)

        api.mergeFails = false
        session.restorePurchases()
        session.restorePurchases()

        assertEquals(listOf("token-anon"), api.merged)
    }

    @Test
    fun anAccountAlreadySignedInHasNothingToMerge() = runTest {
        val session = started()
        auth.provider = "google.com"
        auth.googleAnswer = "other" to null

        session.link(SignInProvider.GOOGLE)

        assertEquals(emptyList(), api.merged)
    }

    // ---------- paying ----------

    @Test
    fun cancellingAtTheStoreChangesNothing() = runTest {
        val session = started()
        purchases.purchaseAnswer = "cancelled" to null

        assertEquals(PurchaseResult.Cancelled, session.purchase(monthly))

        assertEquals(0, api.syncCalls)
        assertFalse(session.status.value.premium)
    }

    @Test
    fun aStoreErrorGrantsNothing() = runTest {
        val session = started()
        purchases.purchaseAnswer = null to "card declined"
        api.statusAfterSync = premium

        assertIs<PurchaseResult.Error>(session.purchase(monthly))

        assertEquals(0, api.syncCalls)
        assertFalse(session.status.value.premium)
    }

    @Test
    fun premiumComesFromTheServerNotFromTheStoreAnswer() = runTest {
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE

        session.purchase(monthly)

        assertFalse(session.status.value.premium)
    }

    @Test
    fun aCoinPackCountsFromTheRealBalance() = runTest {
        api.statusNow = AccountStatus.UNKNOWN_FREE.copy(coins = 5)
        val session = started()

        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 5)
        assertEquals(PurchaseResult.Pending, session.purchase(coinPack))

        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 15)
        assertIs<PurchaseResult.Success>(session.purchase(coinPack))
    }

    /** /status failed at launch, so the app still assumes 0 coins while the seeker really has 5. */
    @Test
    fun aCoinPackIsNotCalledSuccessfulWhenTheBalanceWasNeverKnown() = runTest {
        api.statusNow = null
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 5)

        assertEquals(PurchaseResult.Pending, session.purchase(coinPack))
    }

    @Test
    fun aPaymentTheStoreIsStillWaitingForGrantsNothingAndIsNotAFailure() = runTest {
        val session = started()
        purchases.purchaseAnswer = "pending" to null
        api.statusAfterSync = premium

        assertEquals(PurchaseResult.AwaitingPayment, session.purchase(monthly))

        assertEquals(0, api.syncCalls)
        assertFalse(session.status.value.premium)
    }

    @Test
    fun aRestoreReportsHowManyCoinsItBroughtBack() = runTest {
        api.statusNow = AccountStatus.UNKNOWN_FREE.copy(coins = 5)
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 15)

        assertEquals(10, assertIs<PurchaseResult.Success>(session.restorePurchases()).coinsAdded)
    }

    @Test
    fun aRestoreTheStoreRefusesIsAnErrorAndDoesNotSync() = runTest {
        val session = started()
        purchases.restoreError = "offline"

        assertIs<PurchaseResult.Error>(session.restorePurchases())

        assertEquals(0, api.syncCalls)
    }

    @Test
    fun aRestoreTheServerCannotConfirmIsAnError() = runTest {
        val session = started()
        api.syncFails = true

        assertIs<PurchaseResult.Error>(session.restorePurchases())
    }

    // ---------- deleting the account ----------

    @Test
    fun aDeleteTheServerRefusedWipesNothing() = runTest {
        val session = started()
        api.deleteAccountFails = true

        assertTrue(session.deleteAccount().isFailure)

        assertFalse(journal.cleared)
        assertEquals(0, purchases.logOuts)
        assertEquals("anon", auth.uid)
        assertEquals("anon", session.account.value?.uid)
    }

    @Test
    fun deletingStartsOverAsSomeoneElse() = runTest {
        api.statusNow = premium
        val session = started()
        assertTrue(session.status.value.premium)
        api.statusNow = AccountStatus.UNKNOWN_FREE

        assertTrue(session.deleteAccount().isSuccess)

        assertTrue(journal.cleared)
        assertEquals(1, purchases.logOuts)
        assertEquals("fresh", session.account.value?.uid)
        assertEquals(listOf("anon", "fresh"), purchases.logIns)
        assertFalse(session.status.value.premium)

        api.statusAfterSync = premium
        session.purchase(monthly)
        assertEquals(listOf<String?>("fresh"), purchases.purchasesUnder)
    }

    /** The deletion went through; only the new anonymous session could not start (offline). */
    @Test
    fun aDeleteThatWentThroughIsNotReportedAsFailed() = runTest {
        val session = started()
        auth.nextAnonymousUid = null

        assertTrue(session.deleteAccount().isSuccess, "the account is gone, so telling them it failed is wrong")
    }
}
