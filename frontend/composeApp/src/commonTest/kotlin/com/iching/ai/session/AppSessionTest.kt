package com.iching.ai.session

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.platform.NativeAds
import com.iching.ai.platform.NativeAnalytics
import com.iching.ai.platform.NativeAuth
import com.iching.ai.platform.NativeBridges
import com.iching.ai.platform.NativeNotifications
import com.iching.ai.platform.NativePurchases
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class FakeAuth : NativeAuth {
    override fun currentUserId() = "uid1"
    override fun currentProvider() = "anonymous"
    override fun currentEmail(): String? = null
    override fun signInAnonymously(callback: ResultCallback) = callback.onResult("uid1", null)
    override fun idToken(forceRefresh: Boolean, callback: ResultCallback) = callback.onResult("token", null)
    override fun appCheckToken(callback: ResultCallback) = callback.onResult("ac", null)
    override fun linkApple(callback: ResultCallback) = callback.onResult(null, "unused")
    override fun linkGoogle(callback: ResultCallback) = callback.onResult(null, "unused")
    override fun supportsApple() = false
    override fun supportsGoogle() = false
    override fun signOut() = Unit
    override fun deviceId() = "device-123456"
    override fun appVersion() = "1.0"
}

internal class FakePurchases : NativePurchases {
    var logInWorks = true
    val logIns = mutableListOf<String>()
    var identifiedAs: String? = null
    /** The RevenueCat id each purchase was made under. */
    val purchasesUnder = mutableListOf<String?>()
    override fun logIn(uid: String, callback: ResultCallback) {
        logIns += uid
        if (logInWorks) {
            identifiedAs = uid
            callback.onResult("ok", null)
        } else {
            callback.onResult(null, "offline")
        }
    }
    override fun offerings(callback: ResultCallback) = callback.onResult("[]", null)
    override fun purchase(packageId: String, callback: ResultCallback) {
        purchasesUnder += identifiedAs
        callback.onResult("purchased", null)
    }
    override fun restore(callback: ResultCallback) {
        purchasesUnder += identifiedAs
        callback.onResult("ok", null)
    }
    override fun manageSubscriptionsUrl() = ""
    override fun logOut() {
        identifiedAs = null
    }
}

internal object NoAds : NativeAds {
    override fun showRewarded(callback: ResultCallback) = callback.onResult("dismissed", null)
    override fun preload() = Unit
}

internal object NoAnalytics : NativeAnalytics {
    override fun logEvent(name: String, params: Map<String, String>) = Unit
    override fun setUserId(uid: String?) = Unit
    override fun recordError(message: String) = Unit
}

internal class FakeApi : OracleApi {
    var statusAfterSync = AccountStatus.UNKNOWN_FREE
    var syncFails = false
    var deleteFails = false
    val deleted = mutableListOf<String>()
    var serverReadings = emptyList<Reading>()
    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> = emptyFlow()
    override fun interpretAgain(readingId: String): Flow<DivinationEvent> = emptyFlow()
    override fun followUp(readingId: String, message: String): Flow<DivinationEvent> = emptyFlow()
    override suspend fun status() = AccountStatus.UNKNOWN_FREE
    override suspend fun claimAdReward() = false to AccountStatus.UNKNOWN_FREE
    override suspend fun report(readingId: String, reason: String, note: String) = Unit
    override suspend fun saveOutcome(readingId: String, outcome: Outcome?, note: String?) = Unit
    override suspend fun readings() = serverReadings
    override suspend fun deleteReading(readingId: String) {
        if (deleteFails) throw ApiException(FailureReason.NETWORK, "offline")
        deleted += readingId
    }
    override suspend fun syncPurchases(): AccountStatus {
        if (syncFails) throw ApiException(FailureReason.NETWORK, "offline")
        return statusAfterSync
    }
    override suspend fun deleteAccount() = Unit
}

internal class FakeJournal : JournalRepository {
    val pending = mutableListOf<String>()
    override fun observeAll(): Flow<List<Reading>> = emptyFlow()
    override suspend fun get(id: Long): Reading? = null
    override suspend fun save(reading: Reading) = 0L
    override suspend fun delete(id: Long) = Unit
    override suspend fun markDeleted(serverId: String) {
        pending += serverId
    }
    override suspend fun pendingDeletions() = pending.toList()
    override suspend fun deletionSynced(serverId: String) {
        pending -= serverId
    }
    override suspend fun mergeFromServer(readings: List<Reading>) = 0
    override suspend fun clear() = Unit
}

private object NoSettings : SettingsRepository {
    override val settings: Flow<AppSettings> = emptyFlow()
    override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    override suspend fun clear() = Unit
}

private val monthly = StoreProduct("pkg", "iching_monthly", ProductKind.MONTHLY, "", "฿99", "เดือน", null)
private val coins = StoreProduct("pkg", "iching_coins_10", ProductKind.COINS, "", "฿35", null, null)

class AppSessionTest {
    private val purchases = FakePurchases()
    private val api = FakeApi()
    private val journal = FakeJournal()

    private fun session(scope: CoroutineScope) = FakeAuth().let { auth ->
        AppSession(NativeBridges(auth, purchases, NoAds, NoAnalytics, NoNotifications), FirebaseApiSession(auth), api, journal, NoSettings, scope)
    }

    private fun TestScope.started(): AppSession = session(backgroundScope).also {
        it.start()
        testScheduler.runCurrent()
    }

    @Test
    fun purchaseIdentifiesTheCustomerWhenLoginFailedAtLaunch() = runTest {
        purchases.logInWorks = false
        val session = started()
        purchases.logInWorks = true
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(premium = true)

        assertIs<PurchaseResult.Success>(session.purchase(monthly))

        assertEquals(listOf<String?>("uid1"), purchases.purchasesUnder)
    }

    @Test
    fun nothingIsChargedWhileTheCustomerCannotBeIdentified() = runTest {
        purchases.logInWorks = false
        val session = started()

        assertIs<PurchaseResult.Error>(session.purchase(monthly))

        assertEquals(emptyList(), purchases.purchasesUnder)
    }

    @Test
    fun restoreIdentifiesTheCustomerFirst() = runTest {
        purchases.logInWorks = false
        val session = started()
        purchases.logInWorks = true

        session.restorePurchases()

        assertEquals(listOf<String?>("uid1"), purchases.purchasesUnder)
    }

    @Test
    fun aSuccessfulLoginIsNotRepeatedForEachPurchase() = runTest {
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(premium = true)

        session.purchase(monthly)

        assertEquals(listOf("uid1"), purchases.logIns)
    }

    @Test
    fun subscriptionTheServerDoesNotSeeIsPending() = runTest {
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(premium = false)

        assertEquals(PurchaseResult.Pending, session.purchase(monthly))
    }

    @Test
    fun paidButSyncFailedIsPendingRatherThanAnError() = runTest {
        val session = started()
        api.syncFails = true

        assertEquals(PurchaseResult.Pending, session.purchase(monthly))
    }

    @Test
    fun coinPackNeedsTheBalanceToGoUp() = runTest {
        val session = started()
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 0)
        assertEquals(PurchaseResult.Pending, session.purchase(coins))

        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 10)
        assertIs<PurchaseResult.Success>(session.purchase(coins))
    }

    @Test
    fun pendingDeletionsAreRetriedAtSignIn() = runTest {
        journal.pending += "s1"

        started()

        assertEquals(listOf("s1"), api.deleted)
        assertEquals(emptyList(), journal.pending)
    }

    @Test
    fun aDeletionThatStillFailsStaysPending() = runTest {
        journal.pending += "s1"
        api.deleteFails = true

        started()

        assertEquals(listOf("s1"), journal.pending)
    }
}

private object NoNotifications : NativeNotifications {
    override fun isSupported() = false
    override fun permissionGranted() = false
    override fun requestPermission(callback: ResultCallback) = callback.onResult("denied", null)
    override fun setDailySubscribed(enabled: Boolean, callback: ResultCallback) = callback.onResult("ok", null)
    override fun openSystemSettings() = Unit
}
