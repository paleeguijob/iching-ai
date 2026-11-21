package com.iching.ai.session

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.platform.NativeAnalytics
import com.iching.ai.platform.NativeBridges
import com.iching.ai.platform.NativeNotifications
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.iching.ai.ui.strings.effectiveLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeNotifications : NativeNotifications {
    var granted = false
    var grantOnRequest = true
    var subscribeWorks = true
    var neverAnswers = false
    var throws = false
    /** When set, answers wait here until the test completes them, in order. */
    var hold = false
    val held = ArrayDeque<ResultCallback>()
    val calls = mutableListOf<Boolean>()
    val topics = mutableListOf<String>()
    val others = mutableListOf<String>()
    override fun isSupported() = true
    override fun permissionGranted() = granted
    override fun requestPermission(callback: ResultCallback) {
        granted = grantOnRequest
        callback.onResult(if (granted) "granted" else "denied", null)
    }
    override fun setDailySubscribed(enabled: Boolean, topic: String, otherTopic: String, callback: ResultCallback) {
        calls += enabled
        topics += topic
        others += otherTopic
        if (throws) error("bridge failed")
        if (neverAnswers) return
        if (hold) {
            held += callback
            return
        }
        if (subscribeWorks) callback.onResult("ok", null) else callback.onResult(null, "offline")
    }
    override fun openSystemSettings() = Unit
}

private class RecordingAnalytics : NativeAnalytics {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    override fun logEvent(name: String, params: Map<String, String>) {
        events += name to params
    }
    override fun setUserId(uid: String?) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
    override fun recordError(message: String) = Unit
}

private class MemorySettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    override suspend fun clear() {
        flow.value = AppSettings(onboardingAccepted = flow.value.onboardingAccepted)
    }
}

class AppSessionPushTest {
    private val notifications = FakeNotifications()
    private val analytics = RecordingAnalytics()

    private fun session(settings: SettingsRepository, scope: CoroutineScope) = FakeAuth().let { auth ->
        AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, notifications),
            FirebaseApiSession(auth), FakeApi(), FakeJournal(), settings, scope,
        )
    }

    @Test
    fun enablingSubscribesAndRemembers() = runTest {
        val settings = MemorySettings()
        val result = session(settings, backgroundScope).setDailyPush(true)
        testScheduler.runCurrent()
        assertEquals(DailyPushResult.ENABLED, result)
        assertEquals(listOf(true), notifications.calls)
        assertTrue(settings.flow.value.dailyPushEnabled)
        assertTrue(settings.flow.value.dailyPushSynced)
        assertEquals(listOf("push_toggle" to mapOf("enabled" to "true")), analytics.events)
    }

    @Test
    fun deniedPermissionLeavesPushOff() = runTest {
        notifications.grantOnRequest = false
        val settings = MemorySettings()
        val result = session(settings, backgroundScope).setDailyPush(true)
        assertEquals(DailyPushResult.PERMISSION_DENIED, result)
        assertEquals(emptyList(), notifications.calls)
        assertFalse(settings.flow.value.dailyPushEnabled)
    }

    @Test
    fun disablingUnsubscribes() = runTest {
        notifications.granted = true
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true))
        assertEquals(DailyPushResult.DISABLED, session(settings, backgroundScope).setDailyPush(false))
        testScheduler.runCurrent()
        assertEquals(listOf(false), notifications.calls)
        assertFalse(settings.flow.value.dailyPushEnabled)
    }

    @Test
    fun offlineSubscribeRetriesOnNextStart() = runTest {
        notifications.subscribeWorks = false
        val settings = MemorySettings()
        session(settings, backgroundScope).setDailyPush(true)
        testScheduler.runCurrent()
        assertTrue(settings.flow.value.dailyPushEnabled)
        assertFalse(settings.flow.value.dailyPushSynced)

        notifications.subscribeWorks = true
        session(settings, backgroundScope).start()
        testScheduler.runCurrent()

        assertEquals(listOf(true, true), notifications.calls)
        assertTrue(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun startDoesNotTouchFcmWhenAlreadySynced() = runTest {
        session(MemorySettings(), backgroundScope).start()
        testScheduler.runCurrent()
        assertEquals(emptyList(), notifications.calls)
    }

    @Test
    fun deleteAccountUnsubscribesAndResetsTheme() = runTest {
        val settings = MemorySettings(
            AppSettings(
                onboardingAccepted = true,
                dailyPushEnabled = true,
                selectedTheme = AppTheme.BAMBOO,
                streak = StreakState(8, 8, 100, setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO)),
            ),
        )
        assertTrue(session(settings, backgroundScope).deleteAccount().isSuccess)
        testScheduler.runCurrent()
        assertEquals(listOf(false), notifications.calls)
        assertEquals(AppTheme.INK_PAPER, settings.flow.value.selectedTheme)
        assertEquals(StreakState(), settings.flow.value.streak)
        assertTrue(settings.flow.value.onboardingAccepted)
    }

    @Test
    fun failedUnsubscribeOnDeleteRetriesOnNextStart() = runTest {
        notifications.subscribeWorks = false
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true))
        assertTrue(session(settings, backgroundScope).deleteAccount().isSuccess)
        testScheduler.runCurrent()
        assertFalse(settings.flow.value.dailyPushSynced)

        notifications.subscribeWorks = true
        session(settings, backgroundScope).start()
        testScheduler.runCurrent()

        assertEquals(listOf(false, false), notifications.calls)
        assertTrue(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun deleteAccountFinishesWhenFcmNeverAnswers() = runTest {
        notifications.neverAnswers = true
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true))
        val session = session(settings, backgroundScope)
        val result = withTimeoutOrNull(1_000) { session.deleteAccount() }
        assertTrue(result?.isSuccess == true, "deletion must not wait for FCM")
    }

    @Test
    fun togglingFinishesAndLogsWhenFcmNeverAnswers() = runTest {
        notifications.granted = true
        notifications.neverAnswers = true
        val session = session(MemorySettings(), backgroundScope)
        assertEquals(DailyPushResult.ENABLED, withTimeoutOrNull(1_000) { session.setDailyPush(true) })
        assertEquals(listOf("push_toggle" to mapOf("enabled" to "true")), analytics.events)
    }

    @Test
    fun aThrowingBridgeDoesNotCrashTheLaunch() = runTest {
        notifications.throws = true
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, dailyPushSynced = false))
        session(settings, backgroundScope).start()
        testScheduler.runCurrent()
        assertFalse(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun appVersionComesFromTheInstalledApp() = runTest {
        assertEquals("1.0", session(MemorySettings(), backgroundScope).appVersion)
    }

    @Test
    fun changingLanguageResyncsTheTopicWhenPushIsOn() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true))
        val session = session(settings, backgroundScope)
        session.onLanguageChanged(AppLanguage.EN)
        testScheduler.runCurrent()
        assertEquals(listOf(true), notifications.calls)
        assertEquals(listOf("daily-hexagram-en"), notifications.topics)
        assertTrue(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun changingLanguageLeavesPushOffAlone() = runTest {
        val session = session(MemorySettings(), backgroundScope)
        session.onLanguageChanged(AppLanguage.EN)
        testScheduler.runCurrent()
        assertEquals(emptyList(), notifications.calls)
    }

    @Test
    fun anUnchangedLanguageDoesNotResync() = runTest {
        val session = session(MemorySettings(AppSettings(dailyPushEnabled = true)), backgroundScope)
        session.onLanguageChanged(AppLanguage.TH)
        testScheduler.runCurrent()
        assertEquals(emptyList(), notifications.calls)
    }

    @Test
    fun enablingAfterSwitchingToEnglishSubscribesEnglish() = runTest {
        notifications.granted = true
        val session = session(MemorySettings(), backgroundScope)
        session.onLanguageChanged(AppLanguage.EN)
        session.setDailyPush(true)
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-en"), notifications.topics)
    }

    @Test
    fun switchingLanguageBackAndForthDuringASyncEndsOnTheThaiTopic() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, dailyPushSynced = false))
        val session = session(settings, backgroundScope)
        notifications.hold = true
        backgroundScope.launch { session.start() }
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-th"), notifications.topics)
        backgroundScope.launch { session.onLanguageChanged(AppLanguage.EN) }
        backgroundScope.launch { session.onLanguageChanged(AppLanguage.TH) }
        testScheduler.runCurrent()
        notifications.hold = false
        notifications.held.removeFirst().onResult("ok", null)
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-th"), notifications.topics)
        assertEquals("daily-hexagram-en", notifications.others.last())
        assertTrue(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun aLanguageChangeDuringASyncIsAppliedAfterIt() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, dailyPushSynced = false))
        val session = session(settings, backgroundScope)
        notifications.hold = true
        backgroundScope.launch { session.start() }
        testScheduler.runCurrent()
        backgroundScope.launch { session.onLanguageChanged(AppLanguage.EN) }
        testScheduler.runCurrent()
        notifications.hold = false
        notifications.held.removeFirst().onResult("ok", null)
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-th", "daily-hexagram-en"), notifications.topics)
        assertTrue(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun aFailedLanguageSyncLeavesItUnsynced() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true))
        val session = session(settings, backgroundScope)
        notifications.subscribeWorks = false
        session.onLanguageChanged(AppLanguage.EN)
        testScheduler.runCurrent()
        assertFalse(settings.flow.value.dailyPushSynced)
    }

    @Test
    fun theKillSwitchKeepsAnEnglishSeekerOnTheThaiTopic() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, dailyPushSynced = false, language = AppLanguage.EN))
        val session = session(settings, backgroundScope)
        session.followLanguage(backgroundScope, effectiveLanguage(settings.settings, flowOf(false)))
        session.start()
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-th"), notifications.topics)
        assertEquals(listOf("daily-hexagram-en"), notifications.others)
    }

    @Test
    fun aColdStartInEnglishDoesNotResubscribeWhenSynced() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, language = AppLanguage.EN))
        val session = session(settings, backgroundScope)
        session.followLanguage(backgroundScope, effectiveLanguage(settings.settings, flowOf(true)))
        session.start()
        testScheduler.runCurrent()
        assertEquals(emptyList(), notifications.calls)
    }

    @Test
    fun aColdStartInEnglishSubscribesEnglishWhenUnsynced() = runTest {
        val settings = MemorySettings(AppSettings(dailyPushEnabled = true, dailyPushSynced = false, language = AppLanguage.EN))
        val session = session(settings, backgroundScope)
        session.followLanguage(backgroundScope, effectiveLanguage(settings.settings, flowOf(true)))
        testScheduler.runCurrent()
        session.start()
        testScheduler.runCurrent()
        assertEquals(listOf("daily-hexagram-en"), notifications.topics)
    }
}
