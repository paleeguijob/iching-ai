package com.iching.ai.session

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
    val calls = mutableListOf<Boolean>()
    override fun isSupported() = true
    override fun permissionGranted() = granted
    override fun requestPermission(callback: ResultCallback) {
        granted = grantOnRequest
        callback.onResult(if (granted) "granted" else "denied", null)
    }
    override fun setDailySubscribed(enabled: Boolean, callback: ResultCallback) {
        calls += enabled
        if (throws) error("bridge failed")
        if (neverAnswers) return
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
}
