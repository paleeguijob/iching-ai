package com.iching.ai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Account
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.SignInProvider
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.feature.paywall.restoreMessage
import com.iching.ai.platform.NativeException
import com.iching.ai.session.AppSession
import com.iching.ai.session.DailyPushResult
import com.iching.ai.session.PurchaseResult
import com.iching.ai.ui.strings.AppLanguageHolder
import com.iching.ai.ui.strings.SettingsStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

const val FEEDBACK_MAX_CHARS = 1000

/** Opens the first of [urls] that [open] accepts (it throws when nothing handles a link); returns it, or null. */
internal fun openFirst(urls: List<String>, open: (String) -> Unit): String? =
    urls.firstOrNull { url -> runCatching { open(url) }.isSuccess }

/** The feedback sheet. Closing keeps the draft, so a seeker who taps away mid-sentence loses nothing. */
data class FeedbackUi(val open: Boolean = false, val text: String = "", val sending: Boolean = false, val error: String? = null) {
    val canSend: Boolean get() = text.isNotBlank() && !sending
}

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val session: AppSession,
    private val api: OracleApi,
    private val languageHolder: AppLanguageHolder,
) : ViewModel() {
    private val strings: SettingsStrings get() = languageHolder.strings.settings

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

    /** Store page links for the "Rate the app" row, best first; empty hides the row (iOS until the App Store id exists). */
    val storeUrls: List<String> get() = session.storeUrls

    fun onRateAppTapped() = session.log("rate_app_tap")

    /** What the "Share the app" row sends: a line about the app and its web store page; null hides the row. */
    val shareAppText: String?
        get() = storeUrls.firstOrNull { it.startsWith("https://") }
            ?.let(strings::shareAppText)

    fun onShareAppTapped() = session.log("share_app_tap")

    private val _feedback = MutableStateFlow(FeedbackUi())
    val feedback: StateFlow<FeedbackUi> = _feedback.asStateFlow()

    fun openFeedback() = _feedback.update { it.copy(open = true) }

    fun closeFeedback() = _feedback.update { if (it.sending) it else it.copy(open = false, error = null) }

    fun onFeedbackText(text: String) = _feedback.update { it.copy(text = text.take(FEEDBACK_MAX_CHARS), error = null) }

    fun sendFeedback() {
        val draft = _feedback.value
        if (!draft.canSend) return
        _feedback.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                session.sendFeedback(draft.text.trim())
                _feedback.value = FeedbackUi()
                _message.value = strings.feedbackThanks
                session.log("feedback_sent")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val limit = e is ApiException && e.reason == FailureReason.FEEDBACK_LIMIT
                _feedback.update { it.copy(sending = false, error = if (limit) strings.feedbackLimit else strings.feedbackFailed) }
                session.log("feedback_failed", mapOf("reason" to if (limit) "limit" else "error"))
            }
        }
    }

    private val _yearResult = MutableStateFlow<Int?>(null)

    /** The campaign year the account cast a year hexagram for, or null while there is none. */
    val yearResult: StateFlow<Int?> = _yearResult.asStateFlow()

    init {
        viewModelScope.launch { session.refreshStatus() }
        viewModelScope.launch { _yearResult.value = runCatching { api.yearHexagram() }.getOrNull()?.year }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(soundEnabled = enabled) } }
    }

    val appVersion: String get() = session.appVersion
    val dailyPushSupported: Boolean get() = session.dailyPushSupported

    private val _notificationsGranted = MutableStateFlow(session.notificationsGranted())
    val notificationsGranted: StateFlow<Boolean> = _notificationsGranted.asStateFlow()
    private val _pushBlocked = MutableStateFlow(false)

    /** The system refused: the only way forward is the system's own settings screen. */
    val pushBlocked: StateFlow<Boolean> = _pushBlocked.asStateFlow()

    /** The user may have changed the permission in system settings while away. */
    fun refreshNotificationPermission() {
        _notificationsGranted.value = session.notificationsGranted()
    }

    fun setDailyPush(enabled: Boolean) {
        viewModelScope.launch {
            _pushBlocked.value = session.setDailyPush(enabled) == DailyPushResult.PERMISSION_DENIED
            refreshNotificationPermission()
        }
    }

    fun openNotificationSettings() {
        _pushBlocked.value = false
        session.openNotificationSettings()
    }

    fun dismissPushBlocked() {
        _pushBlocked.value = false
    }

    /** False while English is switched off server-side: the language row stays hidden and everyone reads Thai. */
    val englishEnabled: StateFlow<Boolean> get() = languageHolder.englishEnabled

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { updateSettings { it.copy(language = lang) } }
        session.log("language_selected", mapOf("lang" to lang.wire, "source" to "settings"))
    }

    fun selectTheme(theme: AppTheme) {
        if (theme !in settings.value?.streak?.unlocked.orEmpty()) return
        viewModelScope.launch { updateSettings { it.copy(selectedTheme = theme) } }
        session.log("theme_selected", mapOf("theme" to theme.wire))
    }

    fun link(provider: SignInProvider) = launchBusy {
        _message.value = null
        val result = session.link(provider)
        _message.value = result.fold(
            onSuccess = { linked ->
                if (linked.carriedOver) strings.linkedCarriedOver
                else strings.linkedPartial
            },
            onFailure = { e -> if (e is NativeException && e.message == "cancelled") null else strings.linkFailed },
        )
    }

    fun restore() = launchBusy {
        _message.value = when (val r = session.restorePurchases()) {
            is PurchaseResult.Success -> r.restoreMessage(languageHolder.strings.paywall)
            else -> strings.restoreFailed
        }
    }

    fun syncJournal() = launchBusy {
        _message.value = journalSyncMessage(session.syncJournal(), strings)
    }

    fun deleteAccount(onDone: () -> Unit) = launchBusy {
        session.deleteAccount().fold(
            onSuccess = { onDone() },
            onFailure = { _message.value = strings.deleteFailed },
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

internal fun journalSyncMessage(result: Result<Int>, strings: SettingsStrings): String = result.fold(
    onSuccess = { added -> if (added > 0) strings.syncAdded(added) else strings.syncUpToDate },
    onFailure = { e ->
        if (e is ApiException && e.reason == FailureReason.PREMIUM_REQUIRED) strings.syncPremiumOnly else strings.syncFailed
    },
)
