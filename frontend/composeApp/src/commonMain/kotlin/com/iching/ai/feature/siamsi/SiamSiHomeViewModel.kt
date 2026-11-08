package com.iching.ai.feature.siamsi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.data.siamsi.BUNDLED_SIAMSI_SETS
import com.iching.ai.data.siamsi.resolveSelectedSet
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.SiamSiStatus
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.navigation.SiamSiRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where Home goes after a tap on the Siam Si button. */
sealed interface SiamSiNav {
    data class Draw(val route: SiamSiRoute) : SiamSiNav
    data object Paywall : SiamSiNav
}

/** What an offline launch assumes when the server last said Siam Si was on: the bundled sets, the free quota. */
private val OFFLINE_STATUS = SiamSiStatus(enabled = true, sets = BUNDLED_SIAMSI_SETS.map { SiamSiSetRef(it, 1) }, drawsPerDay = 1)

/** The Siam Si half of Home: the mode switch, the chosen set, and the paid-for draw. */
@OptIn(ExperimentalCoroutinesApi::class)
class SiamSiHomeViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val repository: SiamSiRepository,
    private val gate: SiamSiDrawGate,
    status: StateFlow<AccountStatus>,
    online: StateFlow<Boolean>,
    private val log: (String, Map<String, String>) -> Unit,
) : ViewModel() {
    private val settings = observeSettings()

    /** The server's word once it has answered; before that, what it said last time. */
    private val siamsi: Flow<SiamSiStatus> = combine(status, online, settings.map { it.siamsiLastEnabled }) { s, on, last ->
        if (on || !last) s.siamsi else OFFLINE_STATUS
    }.distinctUntilChanged()

    val siamsiSets: StateFlow<List<SiamSiSet>> = siamsi.map { it.enabled to it.sets }.distinctUntilChanged()
        .mapLatest { (enabled, refs) -> if (enabled) repository.available(refs) else emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The switch shows only when the server allows Siam Si and at least one set is on the device. */
    val enabled: StateFlow<Boolean> = siamsiSets.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Off forces I Ching on screen without overwriting the remembered choice. */
    val mode: StateFlow<DivinationMode> = combine(settings, enabled) { s, on -> if (on) s.divinationMode else DivinationMode.ICHING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DivinationMode.ICHING)

    val selectedSet: StateFlow<SiamSiSet?> = combine(settings.map { it.siamsiSetId }, siamsiSets) { id, sets -> resolveSelectedSet(id, sets) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val drawGate: StateFlow<DrawGateUi> = gate.ui
        .stateIn(viewModelScope, SharingStarted.Eagerly, drawGateUi(false, 1, false, false, false))

    private val nav = Channel<SiamSiNav>(Channel.BUFFERED)
    val navigation: Flow<SiamSiNav> = nav.receiveAsFlow()

    private var starting = false

    init {
        viewModelScope.launch {
            combine(status, online) { s, on -> if (on) s.siamsi.enabled else null }.collect { enabledNow ->
                if (enabledNow != null) updateSettings { if (it.siamsiLastEnabled == enabledNow) it else it.copy(siamsiLastEnabled = enabledNow) }
            }
        }
    }

    fun onModeSelected(mode: DivinationMode) {
        viewModelScope.launch { updateSettings { it.copy(divinationMode = mode) } }
        log("siamsi_mode_selected", mapOf("mode" to mode.wire))
    }

    fun onSetSelected(id: String) {
        viewModelScope.launch { updateSettings { it.copy(siamsiSetId = id) } }
        log("siamsi_set_selected", mapOf("set" to id))
    }

    /** Pays for the draw (an ad for free users), then opens the shake; a second tap meanwhile is ignored. */
    fun onSiamSiStart(question: String, category: QuestionCategory?) {
        if (starting) return
        val set = selectedSet.value ?: return
        starting = true
        viewModelScope.launch {
            try {
                when (gate.requestDraw()) {
                    DrawOutcome.Ready -> nav.send(SiamSiNav.Draw(SiamSiRoute(set.id, question.trim(), category?.wire)))
                    DrawOutcome.LimitReached -> nav.send(SiamSiNav.Paywall)
                    DrawOutcome.Dismissed -> Unit
                }
            } finally {
                starting = false
            }
        }
    }
}
