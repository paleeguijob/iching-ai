package com.iching.ai.session

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.platform.NativeBridges
import com.iching.ai.platform.NativeReview
import com.iching.ai.platform.ResultCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CountingSettings(initial: AppSettings = AppSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
    override suspend fun clear() {
        state.value = AppSettings()
    }
}

/** Records review requests; [error] makes the store's call fail. */
internal class FakeReview(var error: String? = null, private val urls: List<String> = listOf("market://app", "https://play/app")) : NativeReview {
    var requests = 0
    override fun requestReview(callback: ResultCallback) {
        requests++
        callback.onResult(if (error == null) "ok" else null, error)
    }
    override fun storeUrls(): List<String> = urls
    override fun platform() = "android"
}

class AppSessionReviewTest {
    private val api = FakeApi()

    private var now = 1_000_000L

    private fun session(scope: CoroutineScope, settings: SettingsRepository, review: NativeReview) = FakeAuth().let { auth ->
        AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, NoAnalytics, NoNotifications, review), FirebaseApiSession(auth), api,
            FakeJournal(), settings, scope, nowMillis = { now },
        )
    }

    @Test
    fun readingCompletedCountsAndArmsTheFlag() = runTest {
        val settings = CountingSettings()
        val session = session(backgroundScope, settings, FakeReview())
        assertFalse(session.takeReadingFinished())
        session.onReadingCompleted()
        assertEquals(1, settings.state.value.completedReadings)
        assertTrue(session.takeReadingFinished())
        assertFalse(session.takeReadingFinished())
    }

    @Test
    fun aFailedCountWriteNeverBreaksTheReading() = runTest {
        val broken = object : SettingsRepository by CountingSettings() {
            override suspend fun update(transform: (AppSettings) -> AppSettings) = throw IllegalStateException("disk full")
        }
        val session = session(backgroundScope, broken, FakeReview())
        session.onReadingCompleted()
        assertTrue(session.takeReadingFinished())
    }

    @Test
    fun aReadingFinishedLongAgoNoLongerAsks() = runTest {
        val session = session(backgroundScope, CountingSettings(), FakeReview())
        session.onReadingCompleted()
        now += READING_FRESH_MILLIS + 1
        assertFalse(session.takeReadingFinished(), "a later, unrelated return to Home must not ask")
    }

    @Test
    fun requestReviewSaysWhetherTheStoreWasAsked() = runTest {
        assertTrue(session(backgroundScope, CountingSettings(), FakeReview()).requestReview())
        val failing = FakeReview(error = "no activity")
        assertFalse(session(backgroundScope, CountingSettings(), failing).requestReview())
        assertEquals(1, failing.requests)
    }

    @Test
    fun feedbackCarriesTheAppVersionAndPlatform() = runTest {
        session(backgroundScope, CountingSettings(), FakeReview()).sendFeedback("ชอบมาก")
        assertEquals(listOf(Triple("ชอบมาก", "1.0", "android")), api.feedbackSent)
    }

    @Test
    fun storeUrlsComeFromTheBridge() = runTest {
        assertEquals(listOf("market://app", "https://play/app"), session(backgroundScope, CountingSettings(), FakeReview()).storeUrls)
        assertEquals(emptyList(), session(backgroundScope, CountingSettings(), FakeReview(urls = emptyList())).storeUrls)
    }
}
