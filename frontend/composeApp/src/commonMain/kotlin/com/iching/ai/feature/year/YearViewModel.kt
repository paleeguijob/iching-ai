package com.iching.ai.feature.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TOSSES = 6

sealed interface YearUi {
    data object Loading : YearUi

    /** The six ceremonial tosses; the server is asked once the sixth is done. */
    data class Ritual(val tossesDone: Int) : YearUi

    /** [lines] are the six line values (6..9) as the server cast them. */
    data class Result(val hexagram: Hexagram, val lines: List<Int>, val firstTime: Boolean, val year: Int) : YearUi
    data object Offline : YearUi
    data object Inactive : YearUi
}

/**
 * The year hexagram: opens the saved result when the account has one, otherwise runs the ritual.
 * The cast happens on the server so every device shows the same hexagram.
 */
class YearViewModel(
    private val api: OracleApi,
    private val getHexagram: GetHexagramUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val log: (String, Map<String, String>) -> Unit,
    private val campaignId: () -> String,
    /** Brings the Cinnabar Gold theme the server granted on the first cast. */
    private val syncStreak: suspend () -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow<YearUi>(YearUi.Loading)
    val state: StateFlow<YearUi> = _state.asStateFlow()

    val soundEnabled: StateFlow<Boolean> = observeSettings()
        .map { it.soundEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** The six tosses are done and the cast has not come back yet, so retry asks again for the cast. */
    private var castPending = false
    private var busy = false

    init {
        load()
    }

    fun onToss() {
        val ui = _state.value as? YearUi.Ritual ?: return
        if (busy || ui.tossesDone >= TOSSES) return
        val done = ui.tossesDone + 1
        _state.value = YearUi.Ritual(done)
        if (done == TOSSES) {
            castPending = true
            cast()
        }
    }

    fun retry() {
        if (busy) return
        if (castPending) cast() else load()
    }

    /** The first-time notice about the theme has been shown. */
    fun onThemeNoticeShown() = _state.update { if (it is YearUi.Result) it.copy(firstTime = false) else it }

    private fun load() {
        launchStep {
            _state.value = YearUi.Loading
            val existing = api.yearHexagram()
            if (existing == null) YearUi.Ritual(0) else show(existing, firstTime = false)
        }
    }

    private fun cast() {
        launchStep {
            val result = api.castYearHexagram()
            castPending = false
            log("year_cast", mapOf("campaign_id" to campaignId(), "hexagram_id" to result.primaryId.toString()))
            val ui = show(result, firstTime = true)
            // The server granted the theme with the cast; the streak sync is how it reaches this device.
            viewModelScope.launch { syncStreak() }
            ui
        }
    }

    private fun launchStep(step: suspend () -> YearUi) {
        busy = true
        viewModelScope.launch {
            _state.value = try {
                step()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                // A cast that was turned away will not succeed on retry; anything else is worth another try.
                if (e.reason == FailureReason.CAMPAIGN_INACTIVE) YearUi.Inactive.also { castPending = false } else YearUi.Offline
            } catch (e: Exception) {
                YearUi.Offline
            } finally {
                busy = false
            }
        }
    }

    private suspend fun show(result: YearHexagram, firstTime: Boolean): YearUi.Result {
        val hexagram = getHexagram(result.primaryId)
        log("year_view", mapOf("campaign_id" to campaignId()))
        return YearUi.Result(hexagram, result.lines, firstTime, result.year)
    }
}

private val ZODIAC = listOf("ชวด", "ฉลู", "ขาล", "เถาะ", "มะโรง", "มะเส็ง", "มะเมีย", "มะแม", "วอก", "ระกา", "จอ", "กุน")

/** e.g. 2027 → "มะแม ๒๕๗๐": the zodiac animal and the Buddhist-era year. */
fun campaignYearLabel(year: Int): String = "${ZODIAC[(year - 4).mod(12)]} ${(year + 543).toThaiNumerals()}"
