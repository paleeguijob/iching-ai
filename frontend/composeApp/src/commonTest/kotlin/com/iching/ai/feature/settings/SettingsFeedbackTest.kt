package com.iching.ai.feature.settings

import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.ui.strings.thaiLanguageHolder
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.CountingSettings
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FakeReview
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsFeedbackTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val analytics = LoggedAnalytics()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(): SettingsViewModel {
        val auth = FakeAuth()
        val settings = CountingSettings()
        val session = AppSession(
            NativeBridges(auth, FakePurchases(), NoAds, analytics, NoNotifications, FakeReview()),
            FirebaseApiSession(auth), api, FakeJournal(), settings, scope,
        )
        val vm = SettingsViewModel(ObserveSettingsUseCase(settings), UpdateSettingsUseCase(settings), session, api, thaiLanguageHolder())
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun SettingsViewModel.write(text: String) {
        openFeedback()
        onFeedbackText(text)
    }

    private fun logged(event: String) = analytics.events.count { it.first == event }

    @Test
    fun sendsTrimmedFeedbackAndThanks() {
        val vm = viewModel()
        vm.write("  ชอบมาก  ")
        vm.sendFeedback()
        scheduler.advanceUntilIdle()
        assertEquals("ชอบมาก", api.feedbackSent.single().first)
        assertFalse(vm.feedback.value.open)
        assertEquals("", vm.feedback.value.text)
        assertEquals("ขอบคุณสำหรับความเห็น", vm.message.value)
        assertEquals(1, logged("feedback_sent"))
    }

    @Test
    fun keepsTextWhenSendFails() {
        api.feedbackFailure = FailureReason.NETWORK
        val vm = viewModel()
        vm.write("ช้าไปหน่อย")
        vm.sendFeedback()
        scheduler.advanceUntilIdle()
        val ui = vm.feedback.value
        assertTrue(ui.open)
        assertEquals("ช้าไปหน่อย", ui.text)
        assertFalse(ui.sending)
        assertEquals("ส่งไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่", ui.error)
        assertEquals(1, logged("feedback_failed"))
    }

    @Test
    fun showsTheDailyLimit() {
        api.feedbackFailure = FailureReason.FEEDBACK_LIMIT
        val vm = viewModel()
        vm.write("อีกข้อ")
        vm.sendFeedback()
        scheduler.advanceUntilIdle()
        assertEquals("วันนี้ส่งความเห็นครบแล้ว ลองใหม่พรุ่งนี้นะ", vm.feedback.value.error)
    }

    @Test
    fun blankTextCannotBeSent() {
        val vm = viewModel()
        vm.write("   ")
        assertFalse(vm.feedback.value.canSend)
        vm.sendFeedback()
        scheduler.advanceUntilIdle()
        assertTrue(api.feedbackSent.isEmpty())
    }

    @Test
    fun textIsCappedAt1000() {
        val vm = viewModel()
        vm.write("ก".repeat(1200))
        assertEquals(1000, vm.feedback.value.text.length)
    }

    @Test
    fun editingClearsTheErrorAndClosingKeepsTheDraft() {
        api.feedbackFailure = FailureReason.NETWORK
        val vm = viewModel()
        vm.write("ร่าง")
        vm.sendFeedback()
        scheduler.advanceUntilIdle()
        vm.onFeedbackText("ร่างใหม่")
        assertNull(vm.feedback.value.error)
        vm.closeFeedback()
        vm.openFeedback()
        assertEquals("ร่างใหม่", vm.feedback.value.text)
    }

    @Test
    fun rateAppLogsTheTap() {
        val vm = viewModel()
        assertEquals(listOf("market://app", "https://play/app"), vm.storeUrls)
        vm.onRateAppTapped()
        assertEquals(1, logged("rate_app_tap"))
    }

    @Test
    fun theStorePageFallsBackToTheWebWhenThePlayAppIsMissing() {
        val opened = mutableListOf<String>()
        val used = openFirst(listOf("market://app", "https://play/app")) { url ->
            if (url.startsWith("market:")) throw IllegalStateException("no Play app")
            opened += url
        }
        assertEquals("https://play/app", used)
        assertEquals(listOf("https://play/app"), opened)
        assertEquals(null, openFirst(emptyList()) { opened += it })
    }
}
