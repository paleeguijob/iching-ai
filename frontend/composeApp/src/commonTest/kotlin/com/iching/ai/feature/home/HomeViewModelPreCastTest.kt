package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.feature.paywall.AWAITING_PAYMENT_MESSAGE
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeAds
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.LoggedAnalytics
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object OneHexagram : HexagramRepository {
    private val hexagram = Hexagram(
        id = 1, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
    )
    override suspend fun all() = listOf(hexagram)
    override suspend fun byId(id: Int) = hexagram
    override suspend fun byPattern(pattern: String) = hexagram
}

private const val OFFERINGS = """[
  {"packageId":"coins","productId":"iching_coins_10","kind":"coins","price":"฿29"},
  {"packageId":"yearly","productId":"iching_yearly","kind":"yearly","price":"฿390","period":"ปี","trial":"ทดลองฟรี ๗ วัน"}
]"""

private fun status(
    readingsLeft: Int = 0,
    adsWatchableLeft: Int = 2,
    coins: Int = 0,
    premium: Boolean = false,
) = AccountStatus.UNKNOWN_FREE.copy(
    includedLeft = 0, readingsLeft = readingsLeft, adsWatchableLeft = adsWatchableLeft, coins = coins, premium = premium,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPreCastTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)
    private val api = FakeApi()
    private val purchases = FakePurchases().apply { offeringsJson = OFFERINGS }
    private val ads = FakeAds()
    private val analytics = LoggedAnalytics()
    private val casts = mutableListOf<CastRequest>()
    private lateinit var session: AppSession

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    /** [serverStatus] is what GET /status answers; null means the server cannot be reached. */
    private fun viewModel(serverStatus: AccountStatus?, start: Boolean = true): HomeViewModel {
        api.statusNow = serverStatus
        val auth = FakeAuth()
        session = AppSession(
            NativeBridges(auth, purchases, ads, analytics, NoNotifications),
            FirebaseApiSession(auth),
            api,
            FakeJournal(),
            NoSettings,
            scope,
        )
        if (start) session.start()
        val vm = HomeViewModel(GetDailyHexagramUseCase(OneHexagram), ObserveSettingsUseCase(NoSettings), session, api, GetHexagramUseCase(OneHexagram))
        scope.launch { vm.castRequests.collect { casts += it } }
        scheduler.runCurrent()
        return vm
    }

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    private fun HomeViewModel.started(question: String = "ถาม", category: QuestionCategory? = null): HomeViewModel {
        onStart(question, category)
        scheduler.runCurrent()
        return this
    }

    @Test
    fun readingLeftCastsWithoutSheet() {
        val vm = viewModel(status(readingsLeft = 1)).started()
        assertEquals(listOf(CastRequest("ถาม", null)), casts)
        assertNull(vm.sheet.value)
    }

    @Test
    fun freeOutOpensAdSheet() {
        val vm = viewModel(status()).started()
        assertEquals(PreCastSheetKind.AD, vm.sheet.value?.kind)
        assertEquals(2, vm.sheet.value?.adsLeft)
        assertEquals("ถาม", vm.sheet.value?.question)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun adGrantedCastsWithTheOriginalQuestion() {
        val vm = viewModel(status()).started(category = QuestionCategory.WORK)
        api.adGrant = true to status(readingsLeft = 1, adsWatchableLeft = 1)
        vm.onWatchAd()
        scheduler.runCurrent()
        assertNull(vm.sheet.value)
        assertEquals(listOf(CastRequest("ถาม", QuestionCategory.WORK)), casts)
    }

    @Test
    fun adNotFinishedStaysWithMessage() {
        ads.answer = "dismissed"
        val vm = viewModel(status()).started()
        vm.onWatchAd()
        scheduler.runCurrent()
        assertEquals(PreCastSheetKind.AD, vm.sheet.value?.kind)
        assertEquals("ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์", vm.sheet.value?.message)
        assertEquals(false, vm.sheet.value?.busy)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun adLimitTurnsSheetIntoUpsell() {
        val vm = viewModel(status()).started()
        api.adGrant = false to status(adsWatchableLeft = 0)
        vm.onWatchAd()
        scheduler.runCurrent()
        assertEquals(PreCastSheetKind.UPSELL, vm.sheet.value?.kind)
        assertEquals("วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว", vm.sheet.value?.message)
        assertEquals("฿29", vm.sheet.value?.coins?.price)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun nothingLeftOpensUpsellWithStorePrice() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        assertEquals(PreCastSheetKind.UPSELL, vm.sheet.value?.kind)
        assertEquals("฿29", vm.sheet.value?.coins?.price)
        assertEquals("ทดลองฟรี ๗ วัน", vm.sheet.value?.premiumTrial)
    }

    @Test
    fun coinsBoughtCasts() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        api.statusAfterSync = status(adsWatchableLeft = 0, coins = 10, readingsLeft = 10)
        vm.onBuyCoins()
        scheduler.runCurrent()
        assertNull(vm.sheet.value)
        assertEquals(listOf(CastRequest("ถาม", null)), casts)
    }

    @Test
    fun coinsPendingStaysWithMessage() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        purchases.purchaseAnswer = "pending" to null
        vm.onBuyCoins()
        scheduler.runCurrent()
        assertEquals(PreCastSheetKind.UPSELL, vm.sheet.value?.kind)
        assertEquals(AWAITING_PAYMENT_MESSAGE, vm.sheet.value?.message)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun dismissedSheetDoesNotCastAfterPurchase() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        purchases.purchaseAnswer = null
        api.statusAfterSync = status(adsWatchableLeft = 0, coins = 10, readingsLeft = 10)
        vm.onBuyCoins()
        scheduler.runCurrent()
        vm.onSheetDismissed()
        purchases.answerHeld()
        scheduler.runCurrent()
        assertNull(vm.sheet.value)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun scriptureOnlyCasts() {
        val vm = viewModel(status()).started()
        vm.onScriptureOnly()
        scheduler.runCurrent()
        assertNull(vm.sheet.value)
        assertEquals(listOf(CastRequest("ถาม", null)), casts)
    }

    @Test
    fun sheetTurnsReadyWhenReadingsAppear() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        session.updateStatus(status(premium = true, readingsLeft = 10, adsWatchableLeft = 0))
        scheduler.runCurrent()
        assertEquals(PreCastSheetKind.READY, vm.sheet.value?.kind)
        assertTrue(casts.isEmpty())
        vm.onCastFromReady()
        scheduler.runCurrent()
        assertNull(vm.sheet.value)
        assertEquals(listOf(CastRequest("ถาม", null)), casts)
    }

    @Test
    fun slowRefreshFallsBackToKnownStatus() {
        val vm = viewModel(status())
        api.statusDelayMillis = 10_000
        vm.started()
        assertNull(vm.sheet.value)
        advance(1_500)
        assertEquals(PreCastSheetKind.AD, vm.sheet.value?.kind)
        advance(8_500)
        assertEquals(PreCastSheetKind.AD, vm.sheet.value?.kind)
        assertTrue(casts.isEmpty())
    }

    @Test
    fun startIsIgnoredWhileDeciding() {
        val vm = viewModel(status(readingsLeft = 1))
        api.statusDelayMillis = 1_000
        vm.onStart("ถาม", null)
        vm.onStart("ถาม", null)
        advance(1_000)
        assertEquals(1, casts.size)
    }

    /** A second tap while the casting screen slides in, or one replayed on return, must not cast again. */
    @Test
    fun startIsIgnoredUntilHomeResumes() {
        val vm = viewModel(status(readingsLeft = 1)).started()
        vm.started()
        assertEquals(1, casts.size)
        vm.onResumed()
        vm.started()
        assertEquals(2, casts.size)
    }

    @Test
    fun premiumTapThenDismissDoesNotCountALaterPurchase() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        vm.onPremium()
        vm.onSheetDismissed()
        session.updateStatus(status(premium = true, readingsLeft = 10, adsWatchableLeft = 0))
        scheduler.runCurrent()
        assertTrue(analytics.events.none { it.first == "quota_upsell_purchase" })
    }

    @Test
    fun premiumBoughtFromTheSheetIsCountedOnce() {
        val vm = viewModel(status(adsWatchableLeft = 0)).started()
        vm.onPremium()
        session.updateStatus(status(premium = true, readingsLeft = 10, adsWatchableLeft = 0))
        scheduler.runCurrent()
        session.updateStatus(status(premium = true, readingsLeft = 9, adsWatchableLeft = 0))
        scheduler.runCurrent()
        val purchases = analytics.events.filter { it.first == "quota_upsell_purchase" }
        assertEquals(listOf(mapOf("option" to "premium", "placement" to "precast")), purchases.map { it.second })
    }

    @Test
    fun startButtonSaysItIsWaiting() {
        assertEquals("เริ่มเสี่ยงทาย", startButtonText(deciding = false))
        assertEquals("กำลังตั้งจิต...", startButtonText(deciding = true))
    }

    @Test
    fun offlineCastsStraightAway() {
        viewModel(serverStatus = null, start = false).started()
        assertEquals(listOf(CastRequest("ถาม", null)), casts)
    }
}
