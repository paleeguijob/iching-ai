package com.iching.ai.feature.paywall

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FakeApi
import com.iching.ai.session.FakeAuth
import com.iching.ai.session.FakeJournal
import com.iching.ai.session.FakePurchases
import com.iching.ai.session.FirebaseApiSession
import com.iching.ai.session.NoAds
import com.iching.ai.session.NoAnalytics
import com.iching.ai.session.NoNotifications
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val monthly = StoreProduct("m", "iching_premium_monthly", ProductKind.MONTHLY, "", "฿79", "เดือน", null)
private val coinPack = StoreProduct("c", "iching_coins_10", ProductKind.COINS, "", "฿35", null, null)
private const val NO_SUBSCRIPTION = "ไม่พบการสมัครสมาชิกที่ยังใช้งานอยู่"

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {
    private val purchases = FakePurchases()
    private val api = FakeApi()

    private fun viewModel(scope: CoroutineScope) = FakeAuth().let { auth ->
        PaywallViewModel(AppSession(NativeBridges(auth, purchases, NoAds, NoAnalytics, NoNotifications), FirebaseApiSession(auth), api, FakeJournal(), NoSettings, scope))
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aDoubleTapPaysOnce() = runTest {
        val vm = viewModel(backgroundScope)
        purchases.purchaseAnswer = null
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(premium = true)

        vm.buy(monthly)
        vm.buy(monthly)
        vm.restore()
        purchases.answerHeld()

        assertEquals(1, purchases.purchasesUnder.size)
        assertTrue(vm.state.value.purchased)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun cancellingSaysNothing() = runTest {
        val vm = viewModel(backgroundScope)
        purchases.purchaseAnswer = "cancelled" to null

        vm.buy(monthly)

        assertNull(vm.state.value.message.takeIf { it != STORE_UNAVAILABLE })
        assertFalse(vm.state.value.purchased)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun aFailedPaymentIsNotShownAsPurchased() = runTest {
        val vm = viewModel(backgroundScope)
        purchases.purchaseAnswer = null to "declined"

        vm.buy(monthly)

        assertEquals("การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง", vm.state.value.message)
        assertFalse(vm.state.value.purchased)
    }

    @Test
    fun paidButUnconfirmedWarnsAgainstBuyingAgain() = runTest {
        val vm = viewModel(backgroundScope)
        api.syncFails = true

        vm.buy(monthly)

        assertTrue(vm.state.value.message!!.contains("โปรดอย่าซื้อซ้ำ"))
        assertFalse(vm.state.value.purchased)
    }

    @Test
    fun aPaymentStillAwaitedAsksThemNotToBuyAgainWithoutClaimingItWasPaid() = runTest {
        val vm = viewModel(backgroundScope)
        purchases.purchaseAnswer = "pending" to null

        vm.buy(monthly)

        assertTrue(vm.state.value.message!!.contains("โปรดอย่าซื้อซ้ำ"))
        assertFalse(vm.state.value.message!!.contains("ชำระเงินแล้ว"))
        assertFalse(vm.state.value.purchased)
    }

    /** The pending message sends coin buyers to restore, which must not answer that nothing was found. */
    @Test
    fun restoringAfterAPendingCoinPackAcknowledgesTheCoins() = runTest {
        val vm = viewModel(backgroundScope)
        api.syncFails = true
        vm.buy(coinPack)

        api.syncFails = false
        api.statusAfterSync = AccountStatus.UNKNOWN_FREE.copy(coins = 10)
        vm.restore()

        assertNotEquals(NO_SUBSCRIPTION, vm.state.value.message)
    }

    private companion object {
        const val STORE_UNAVAILABLE = "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"
    }
}
