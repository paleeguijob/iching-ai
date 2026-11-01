package com.iching.ai.feature.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.session.AppSession
import com.iching.ai.session.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaywallUiState(
    val loading: Boolean = true,
    val products: List<StoreProduct> = emptyList(),
    val selected: StoreProduct? = null,
    val busy: Boolean = false,
    val status: AccountStatus = AccountStatus.UNKNOWN_FREE,
    val message: String? = null,
    val purchased: Boolean = false,
) {
    val subscriptions: List<StoreProduct> get() = products.filter { it.kind != ProductKind.COINS }
    val coins: StoreProduct? get() = products.firstOrNull { it.kind == ProductKind.COINS }
}

private const val PENDING_MESSAGE = "ชำระเงินแล้ว แต่ระบบยังไม่อัปเดตสิทธิ์ โปรดอย่าซื้อซ้ำ กดกู้คืนการซื้ออีกครั้งในอีกสักครู่"

class PaywallViewModel(private val session: AppSession) : ViewModel() {
    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    val manageSubscriptionsUrl: String get() = session.manageSubscriptionsUrl

    init {
        session.log("paywall_viewed")
        viewModelScope.launch { session.status.collect { s -> _state.update { it.copy(status = s) } } }
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val products = runCatching { session.products() }.getOrElse { emptyList() }
            _state.update {
                it.copy(
                    loading = false,
                    products = products,
                    // Lead with the yearly plan (with its free trial).
                    selected = products.firstOrNull { p -> p.kind == ProductKind.YEARLY } ?: products.firstOrNull(),
                    message = if (products.isEmpty()) "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่" else null,
                )
            }
        }
    }

    fun select(product: StoreProduct) = _state.update { it.copy(selected = product) }

    fun buy(product: StoreProduct? = _state.value.selected) {
        if (product == null || _state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = session.purchase(product)
            _state.update {
                when (result) {
                    is PurchaseResult.Success -> it.copy(
                        busy = false,
                        purchased = true,
                        message = if (product.kind == ProductKind.COINS) "ได้รับเหรียญแล้ว ขอบคุณที่สนับสนุน" else "ยินดีต้อนรับสู่ Premium",
                    )
                    PurchaseResult.Cancelled -> it.copy(busy = false)
                    PurchaseResult.Pending -> it.copy(busy = false, message = PENDING_MESSAGE)
                    is PurchaseResult.Error -> it.copy(busy = false, message = "การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง")
                }
            }
        }
    }

    fun restore() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = session.restorePurchases()
            _state.update {
                when (result) {
                    is PurchaseResult.Success -> it.copy(
                        busy = false,
                        purchased = result.status.premium,
                        message = if (result.status.premium) "กู้คืนสมาชิก Premium แล้ว" else "ไม่พบการสมัครสมาชิกที่ยังใช้งานอยู่",
                    )
                    PurchaseResult.Cancelled -> it.copy(busy = false)
                    PurchaseResult.Pending -> it.copy(busy = false, message = PENDING_MESSAGE)
                    is PurchaseResult.Error -> it.copy(busy = false, message = "กู้คืนไม่สำเร็จ โปรดลองใหม่อีกครั้ง")
                }
            }
        }
    }
}
