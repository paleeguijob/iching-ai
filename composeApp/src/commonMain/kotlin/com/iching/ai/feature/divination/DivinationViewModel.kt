package com.iching.ai.feature.divination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.usecase.FallbackReason
import com.iching.ai.domain.usecase.InterpretReadingUseCase
import com.iching.ai.domain.usecase.InterpretationUpdate
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class DivinationPhase { CASTING, REVEAL, INTERPRETING, COMPLETE }

data class DivinationUiState(
    val question: String,
    val tosses: List<CoinToss> = emptyList(),
    /** The toss currently in the air; drives the coin animation. */
    val currentToss: CoinToss? = null,
    val tossKey: Int = 0,
    val isTossing: Boolean = false,
    val phase: DivinationPhase = DivinationPhase.CASTING,
    val cast: CastResult? = null,
    val interpretation: String = "",
    val source: InterpretationSource? = null,
    val fallbackReason: FallbackReason? = null,
    val savedReadingId: Long? = null,
    val createdAtMillis: Long = 0,
    val soundEnabled: Boolean = true,
    val error: String? = null,
) {
    val tossesDone: Int get() = tosses.size
    val canToss: Boolean get() = phase == DivinationPhase.CASTING && !isTossing && tosses.size < 6
}

class DivinationViewModel(
    question: String,
    private val tossCoins: TossCoinsUseCase,
    private val resolveCast: ResolveCastUseCase,
    private val interpretReading: InterpretReadingUseCase,
    private val saveReading: SaveReadingUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DivinationUiState(question = question.trim()))
    val state: StateFlow<DivinationUiState> = _state.asStateFlow()

    private var interpretJob: Job? = null
    private var autoTossJob: Job? = null

    init {
        viewModelScope.launch {
            observeSettings().collect { s -> _state.update { it.copy(soundEnabled = s.soundEnabled) } }
        }
    }

    fun onToss() {
        if (!_state.value.canToss) return
        val toss = tossCoins()
        _state.update { it.copy(currentToss = toss, tossKey = it.tossKey + 1, isTossing = true) }
        viewModelScope.launch {
            delay(TOSS_ANIMATION_MILLIS)
            _state.update { it.copy(tosses = it.tosses + toss, isTossing = false) }
            if (_state.value.tosses.size == 6) finishCast()
        }
    }

    /** Tosses every remaining line in sequence, for seekers who prefer not to tap six times. */
    fun onTossRemaining() {
        if (autoTossJob?.isActive == true) return
        autoTossJob = viewModelScope.launch {
            while (_state.value.tosses.size < 6 && _state.value.phase == DivinationPhase.CASTING) {
                if (_state.value.canToss) onToss()
                delay(TOSS_ANIMATION_MILLIS + 250)
            }
        }
    }

    fun onRetryInterpretation() {
        if (_state.value.cast == null || interpretJob?.isActive == true) return
        _state.update { it.copy(interpretation = "", source = null, fallbackReason = null) }
        interpret()
    }

    private suspend fun finishCast() {
        try {
            val cast = resolveCast(_state.value.tosses.map { it.line })
            _state.update {
                it.copy(
                    cast = cast,
                    phase = DivinationPhase.REVEAL,
                    createdAtMillis = Clock.System.now().toEpochMilliseconds(),
                )
            }
            delay(REVEAL_MILLIS)
            interpret()
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "ไม่สามารถเปิดคัมภีร์ได้") }
        }
    }

    private fun interpret() {
        val cast = _state.value.cast ?: return
        _state.update { it.copy(phase = DivinationPhase.INTERPRETING) }
        interpretJob = viewModelScope.launch {
            interpretReading(_state.value.question, cast).collect { update ->
                when (update) {
                    is InterpretationUpdate.Streaming -> _state.update { it.copy(interpretation = update.text) }
                    is InterpretationUpdate.Completed -> {
                        _state.update {
                            it.copy(
                                interpretation = update.text,
                                source = update.source,
                                fallbackReason = update.fallbackReason,
                                phase = DivinationPhase.COMPLETE,
                            )
                        }
                        persist(cast)
                    }
                }
            }
        }
    }

    /** Writes the reading to the Destiny Journal; a retry updates the same entry. */
    private suspend fun persist(cast: CastResult) {
        val s = _state.value
        val id = saveReading(
            Reading(
                id = s.savedReadingId ?: 0,
                question = s.question,
                createdAtMillis = s.createdAtMillis,
                lines = cast.lines,
                primaryId = cast.primary.id,
                relatingId = cast.relating?.id,
                interpretation = s.interpretation,
                source = s.source ?: InterpretationSource.SCRIPTURE,
            )
        )
        _state.update { it.copy(savedReadingId = id) }
    }

    companion object {
        const val TOSS_ANIMATION_MILLIS = 1350L
        const val REVEAL_MILLIS = 2200L
    }
}
