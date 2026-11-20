package com.iching.ai.feature.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.session.AppSession
import com.iching.ai.session.PurchaseResult
import com.iching.ai.ui.strings.AppLanguageHolder
import com.iching.ai.ui.strings.PaywallStrings
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

/** What restoring brought back; a coin pack counts, not only a subscription. */
fun PurchaseResult.Success.restoreMessage(strings: PaywallStrings): String = when {
    status.premium -> strings.restoredPremium
    coinsAdded > 0 -> strings.restoredCoins(coinsAdded)
    else -> strings.restoreNothing
}

class PaywallViewModel(
    private val session: AppSession,
    private val languageHolder: AppLanguageHolder,
) : ViewModel() {
    private val strings: PaywallStrings get() = languageHolder.strings.paywall

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
                    message = if (products.isEmpty()) strings.productsFailed else null,
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
                        message = if (product.kind == ProductKind.COINS) strings.coinsReceived else strings.welcomePremium,
                    )
                    PurchaseResult.Cancelled -> it.copy(busy = false)
                    PurchaseResult.Pending -> it.copy(busy = false, message = strings.pending)
                    PurchaseResult.AwaitingPayment -> it.copy(busy = false, message = strings.awaitingPayment)
                    is PurchaseResult.Error -> it.copy(busy = false, message = languageHolder.strings.common.paymentFailed)
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
                        purchased = result.status.premium || result.coinsAdded > 0,
                        message = result.restoreMessage(strings),
                    )
                    PurchaseResult.Cancelled -> it.copy(busy = false)
                    PurchaseResult.Pending -> it.copy(busy = false, message = strings.pending)
                    PurchaseResult.AwaitingPayment -> it.copy(busy = false, message = strings.awaitingPayment)
                    is PurchaseResult.Error -> it.copy(busy = false, message = strings.restoreFailed)
                }
            }
        }
    }
}
