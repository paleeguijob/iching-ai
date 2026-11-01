package com.iching.ai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Account
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.SignInProvider
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.platform.NativeException
import com.iching.ai.session.AppSession
import com.iching.ai.session.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val session: AppSession,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val status: StateFlow<AccountStatus> = session.status
    val account: StateFlow<Account?> = session.account

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val supportsApple: Boolean get() = session.supportsApple
    val supportsGoogle: Boolean get() = session.supportsGoogle
    val manageSubscriptionsUrl: String get() = session.manageSubscriptionsUrl

    init {
        viewModelScope.launch { session.refreshStatus() }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(soundEnabled = enabled) } }
    }

    fun link(provider: SignInProvider) = launchBusy {
        _message.value = null
        val result = session.link(provider)
        _message.value = result.fold(
            onSuccess = { "เชื่อมบัญชีแล้ว การซื้อและบันทึกชะตาจะติดตามท่านไปยังเครื่องใหม่" },
            onFailure = { e -> if (e is NativeException && e.message == "cancelled") null else "เชื่อมบัญชีไม่สำเร็จ โปรดลองใหม่อีกครั้ง" },
        )
    }

    fun restore() = launchBusy {
        _message.value = when (val r = session.restorePurchases()) {
            is PurchaseResult.Success -> if (r.status.premium) "กู้คืนสมาชิก Premium แล้ว" else "ไม่พบการสมัครสมาชิกที่ยังใช้งานอยู่"
            else -> "กู้คืนไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
        }
    }

    fun syncJournal() = launchBusy {
        val added = session.syncJournal()
        _message.value = if (added > 0) "ดึงบันทึกชะตาจากระบบมา $added รายการ" else "บันทึกชะตาในเครื่องเป็นปัจจุบันแล้ว"
    }

    fun deleteAccount(onDone: () -> Unit) = launchBusy {
        session.deleteAccount().fold(
            onSuccess = { onDone() },
            onFailure = { _message.value = "ลบบัญชีไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่" },
        )
    }

    fun messageShown() {
        _message.value = null
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.update { true }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.update { false }
            }
        }
    }
}
