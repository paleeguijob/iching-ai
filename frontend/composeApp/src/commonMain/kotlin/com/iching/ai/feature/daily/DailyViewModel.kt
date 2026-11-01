package com.iching.ai.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.usecase.ClaimPushPromptUseCase
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.RecordDailyViewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class DailyUiState(
    val hexagram: Hexagram? = null,
    val streak: StreakState = StreakState(),
    val newlyUnlocked: List<AppTheme> = emptyList(),
    val showPushPrompt: Boolean = false,
)

class DailyViewModel(
    private val getDaily: GetDailyHexagramUseCase,
    private val recordView: RecordDailyViewUseCase,
    private val claimPushPrompt: ClaimPushPromptUseCase,
    private val log: (String, Map<String, String>) -> Unit,
    private val pushSupported: () -> Boolean,
    private val enablePush: suspend () -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    private val _state = MutableStateFlow(DailyUiState())
    val state: StateFlow<DailyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Also runs on every resume: the screen may have stayed open past midnight. */
    fun refresh() {
        viewModelScope.launch {
            val nowMillis = now()
            val hexagram = getDaily(nowMillis)
            val update = recordView(nowMillis)
            if (update.counted) log("daily_view", emptyMap())
            update.newlyUnlocked.forEach { log("streak_milestone", mapOf("days" to it.unlockDays.toString())) }
            val offerPush = pushSupported() && claimPushPrompt()
            _state.update {
                it.copy(
                    hexagram = hexagram,
                    streak = update.state,
                    newlyUnlocked = it.newlyUnlocked + update.newlyUnlocked,
                    showPushPrompt = it.showPushPrompt || offerPush,
                )
            }
        }
    }

    fun logEvent(name: String, params: Map<String, String>) = log(name, params)

    fun unlockNoticeShown() = _state.update { it.copy(newlyUnlocked = emptyList()) }

    fun dismissPushPrompt() = _state.update { it.copy(showPushPrompt = false) }

    fun acceptPushPrompt() {
        dismissPushPrompt()
        viewModelScope.launch { enablePush() }
    }
}
