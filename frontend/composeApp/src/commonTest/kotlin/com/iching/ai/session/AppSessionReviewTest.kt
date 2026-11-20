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
internal class FakeReview(var error: String? = null, private val url: String? = "https://play/app") : NativeReview {
    var requests = 0
    override fun requestReview(callback: ResultCallback) {
        requests++
        callback.onResult(if (error == null) "ok" else null, error)
    }
    override fun storeUrl(): String? = url
    override fun platform() = "android"
}

class AppSessionReviewTest {
    private val api = FakeApi()

    private fun session(scope: CoroutineScope, settings: SettingsRepository, review: NativeReview) = FakeAuth().let { auth ->
        AppSession(NativeBridges(auth, FakePurchases(), NoAds, NoAnalytics, NoNotifications, review), FirebaseApiSession(auth), api, FakeJournal(), settings, scope)
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
    fun reviewFailureIsSwallowed() = runTest {
        val review = FakeReview(error = "no activity")
        session(backgroundScope, CountingSettings(), review).requestReview()
        assertEquals(1, review.requests)
    }

    @Test
    fun feedbackCarriesTheAppVersionAndPlatform() = runTest {
        session(backgroundScope, CountingSettings(), FakeReview()).sendFeedback("ชอบมาก")
        assertEquals(listOf(Triple("ชอบมาก", "1.0", "android")), api.feedbackSent)
    }

    @Test
    fun storeUrlComesFromTheBridge() = runTest {
        assertEquals("https://play/app", session(backgroundScope, CountingSettings(), FakeReview()).storeUrl)
        assertEquals(null, session(backgroundScope, CountingSettings(), FakeReview(url = null)).storeUrl)
    }
}
