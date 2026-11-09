package com.iching.ai.session

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import com.iching.ai.domain.model.mergedWith
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.YearHexagram
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
    var restores = 0
    var logOuts = 0
    /** What the store answers: value or error. Null holds the answer back until [answerHeld]. */
    var purchaseAnswer: Pair<String?, String?>? = "purchased" to null
    var restoreError: String? = null
    private var held: ResultCallback? = null
    fun answerHeld() = held?.onResult("purchased", null)
    override fun logIn(uid: String, callback: ResultCallback) {
        logIns += uid
        if (logInWorks) {
            identifiedAs = uid
            callback.onResult("ok", null)
        } else {
            callback.onResult(null, "offline")
        }
    }
    /** The store's packages, as the native bridge reports them. */
    var offeringsJson = "[]"
    override fun offerings(callback: ResultCallback) = callback.onResult(offeringsJson, null)
    override fun purchase(packageId: String, callback: ResultCallback) {
        purchasesUnder += identifiedAs
        val answer = purchaseAnswer
        if (answer == null) held = callback else callback.onResult(answer.first, answer.second)
    }
    override fun restore(callback: ResultCallback) {
        purchasesUnder += identifiedAs
        restores++
        restoreError?.let { callback.onResult(null, it) } ?: callback.onResult("ok", null)
    }
    override fun manageSubscriptionsUrl() = ""
    override fun logOut() {
        logOuts++
        identifiedAs = null
    }
}

internal object NoAds : NativeAds {
    override fun showRewarded(callback: ResultCallback) = callback.onResult("dismissed", null)
    override fun preload() = Unit
}

/** A rewarded ad that ends with [answer] ("rewarded" when watched to the end); null fails to show. */
internal class FakeAds(var answer: String? = "rewarded") : NativeAds {
    override fun showRewarded(callback: ResultCallback) = answer?.let { callback.onResult(it, null) } ?: callback.onResult(null, "no fill")
    override fun preload() = Unit
}

internal class LoggedAnalytics : NativeAnalytics {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    override fun logEvent(name: String, params: Map<String, String>) {
        events += name to params
    }
    override fun setUserId(uid: String?) = Unit
    override fun recordError(message: String) = Unit
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
    /** GET /status; null answers as if the server cannot be reached. */
    var statusNow: AccountStatus? = AccountStatus.UNKNOWN_FREE
    var syncCalls = 0
    var deleteAccountFails = false
    var deleteAccountCalls = 0
    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> = emptyFlow()
    override fun interpretAgain(readingId: String): Flow<DivinationEvent> = emptyFlow()
    override fun followUp(readingId: String, message: String): Flow<DivinationEvent> = emptyFlow()
    var yearHexagramNow: YearHexagram? = null
    var yearFailure: FailureReason? = null
    /** What POST /year-hexagram answers; falls back to [yearHexagramNow]. */
    var castYearResult: YearHexagram? = null
    var castYearFailure: FailureReason? = null
    var castYearCalls = 0
    var yearHexagramCalls = 0
    override suspend fun yearHexagram(): YearHexagram? {
        yearHexagramCalls++
        yearFailure?.let { throw ApiException(it, "failed") }
        return yearHexagramNow
    }
    override suspend fun castYearHexagram(): YearHexagram {
        castYearCalls++
        castYearFailure?.let { throw ApiException(it, "failed") }
        val result = castYearResult ?: yearHexagramNow ?: throw ApiException(FailureReason.CAMPAIGN_INACTIVE, "inactive")
        yearHexagramNow = result
        return result
    }
    override fun divineYear(question: String): Flow<DivinationEvent> = emptyFlow()
    /** How long GET /status takes to answer. */
    var statusDelayMillis = 0L
    override suspend fun status(): AccountStatus {
        if (statusDelayMillis > 0) delay(statusDelayMillis)
        return statusNow ?: throw ApiException(FailureReason.NETWORK, "offline")
    }
    /** What POST /ad-reward answers: granted, and the status after it. */
    var adGrant: Pair<Boolean, AccountStatus> = false to AccountStatus.UNKNOWN_FREE
    override suspend fun claimAdReward() = adGrant
    override suspend fun report(readingId: String, reason: String, note: String) = Unit
    var saveOutcomeFailure: FailureReason? = null
    val outcomesSaved = mutableListOf<String>()
    override suspend fun saveOutcome(readingId: String, outcome: Outcome?, note: String?) {
        saveOutcomeFailure?.let { throw ApiException(it, "failed") }
        outcomesSaved += readingId
    }
    var serverStreak = StreakState()
    var streakFails = false
    val streaksSent = mutableListOf<StreakState>()
    override suspend fun syncStreak(streak: StreakState): StreakState {
        if (streakFails) throw ApiException(FailureReason.NETWORK, "offline")
        streaksSent += streak
        return serverStreak.mergedWith(streak).also { serverStreak = it }
    }
    var readingsFailure: FailureReason? = null
    override suspend fun readings(): List<Reading> {
        readingsFailure?.let { throw ApiException(it, "failed") }
        return serverReadings
    }
    override suspend fun deleteReading(readingId: String) {
        if (deleteFails) throw ApiException(FailureReason.NETWORK, "offline")
        deleted += readingId
    }
    override suspend fun syncPurchases(): AccountStatus {
        syncCalls++
        if (syncFails) throw ApiException(FailureReason.NETWORK, "offline")
        return statusAfterSync
    }
    var mergeFails = false
    val merged = mutableListOf<String>()
    override suspend fun mergeAccount(fromIdToken: String): AccountStatus {
        if (mergeFails) throw ApiException(FailureReason.NETWORK, "offline")
        merged += fromIdToken
        return statusAfterSync
    }
    override suspend fun deleteAccount() {
        deleteAccountCalls++
        if (deleteAccountFails) throw ApiException(FailureReason.NETWORK, "offline")
    }
}

internal class FakeJournal : JournalRepository {
    val pending = mutableListOf<String>()
    var cleared = false
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
    val unsynced = mutableListOf<Reading>()
    override suspend fun markOutcomeUnsynced(serverId: String) = Unit
    override suspend fun unsyncedOutcomes() = unsynced.toList()
    override suspend fun outcomeSynced(serverId: String) {
        unsynced.removeAll { it.serverId == serverId }
    }
    val merged = mutableListOf<Reading>()
    override suspend fun mergeFromServer(readings: List<Reading>): Int {
        merged += readings
        return readings.size
    }
    override suspend fun clear() {
        cleared = true
    }
}

private class StreakSettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
    override suspend fun clear() {
        state.value = AppSettings()
    }
}

internal object NoSettings : SettingsRepository {
    override val settings: Flow<AppSettings> = emptyFlow()
    override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    override suspend fun clear() = Unit
}

private val monthly = StoreProduct("pkg", "iching_monthly", ProductKind.MONTHLY, "", "฿99", "เดือน", null)
private val coins = StoreProduct("pkg", "iching_coins_10", ProductKind.COINS, "", "฿29", null, null)

class AppSessionTest {
    private val purchases = FakePurchases()
    private val api = FakeApi()
    private val journal = FakeJournal()

    private fun session(scope: CoroutineScope, settings: SettingsRepository = NoSettings) = FakeAuth().let { auth ->
        AppSession(NativeBridges(auth, purchases, NoAds, NoAnalytics, NoNotifications), FirebaseApiSession(auth), api, journal, settings, scope)
    }

    private fun TestScope.started(settings: SettingsRepository = NoSettings): AppSession = session(backgroundScope, settings).also {
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

    private val unsyncedReading = Reading(
        question = "q", createdAtMillis = 1, lines = List(6) { LineType.YOUNG_YANG }, primaryId = 1, relatingId = null,
        interpretation = "text", source = InterpretationSource.AI, serverId = "s1", outcome = Outcome.YES,
    )

    @Test
    fun unsavedOutcomesAreRetriedAtSignIn() = runTest {
        journal.unsynced += unsyncedReading

        started()

        assertEquals(listOf("s1"), api.outcomesSaved)
        assertEquals(emptyList(), journal.unsynced)
    }

    @Test
    fun anOutcomeThatStillFailsStaysUnsynced() = runTest {
        journal.unsynced += unsyncedReading
        api.saveOutcomeFailure = FailureReason.NETWORK

        started()

        assertEquals(listOf(unsyncedReading), journal.unsynced)
    }

    @Test
    fun anOutcomeForAReadingGoneFromTheServerIsDropped() = runTest {
        journal.unsynced += unsyncedReading
        api.saveOutcomeFailure = FailureReason.NOT_FOUND

        started()

        assertEquals(emptyList(), journal.unsynced)
    }

    @Test
    fun aReinstallGetsTheStreakAndThemesBackAtSignIn() = runTest {
        val kept = StreakState(current = 8, best = 30, lastEpochDay = 100, unlocked = setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO, AppTheme.CINNABAR_DAWN))
        api.serverStreak = kept
        val settings = StreakSettings()

        started(settings)

        assertEquals(kept, settings.state.value.streak)
    }

    @Test
    fun thisDevicesStreakIsSentToTheAccount() = runTest {
        val here = StreakState(current = 7, best = 7, lastEpochDay = 100, unlocked = setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO))
        val session = started(StreakSettings(AppSettings(streak = here)))

        assertEquals(listOf(here), api.streaksSent)
        session.syncStreak()
        assertEquals(here, api.serverStreak)
    }

    @Test
    fun anOfflineStreakSyncLeavesTheDeviceStreakAlone() = runTest {
        api.streakFails = true
        val here = StreakState(current = 3, best = 3, lastEpochDay = 100)
        val settings = StreakSettings(AppSettings(streak = here))

        started(settings)

        assertEquals(here, settings.state.value.streak)
    }

    @Test
    fun aFreeAccountGetsItsServerReadingsBackAtSignIn() = runTest {
        api.serverReadings = listOf(unsyncedReading)

        started()

        assertEquals(listOf(unsyncedReading), journal.merged)
    }

    @Test
    fun syncJournalCountsWhatItAdded() = runTest {
        val session = started()
        api.serverReadings = listOf(unsyncedReading)

        assertEquals(1, session.syncJournal().getOrNull())
    }

    @Test
    fun aFailedJournalFetchIsReportedAsAFailure() = runTest {
        val session = started()
        api.readingsFailure = FailureReason.NETWORK

        assertTrue(session.syncJournal().isFailure)
    }

    @Test
    fun aJournalFetchRefusedForLapsedPremiumUpdatesTheStatus() = runTest {
        api.statusNow = AccountStatus.UNKNOWN_FREE.copy(premium = true)
        val session = started()
        api.statusNow = AccountStatus.UNKNOWN_FREE
        api.readingsFailure = FailureReason.PREMIUM_REQUIRED

        session.syncJournal()

        assertEquals(false, session.status.value.premium)
    }

    @Test
    fun aDeletionThatStillFailsStaysPending() = runTest {
        journal.pending += "s1"
        api.deleteFails = true

        started()

        assertEquals(listOf("s1"), journal.pending)
    }
}

internal object NoNotifications : NativeNotifications {
    override fun isSupported() = false
    override fun permissionGranted() = false
    override fun requestPermission(callback: ResultCallback) = callback.onResult("denied", null)
    override fun setDailySubscribed(enabled: Boolean, callback: ResultCallback) = callback.onResult("ok", null)
    override fun openSystemSettings() = Unit
}
