package com.iching.ai.feature.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.ui.toThaiNumerals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TOSSES = 6
private const val TOSS_ANIMATION_MILLIS = 900L
private const val LAST_TOSS_HOLD_MILLIS = 900L

sealed interface YearUi {
    data object Loading : YearUi

    /** The ritual: the tosses revealed so far, the one in the air ([currentToss]), and whether the server's answer is awaited. */
    data class Ritual(
        val tosses: List<CoinToss> = emptyList(),
        val currentToss: CoinToss? = null,
        val tossKey: Int = 0,
        val isTossing: Boolean = false,
        val awaitingCast: Boolean = false,
    ) : YearUi {
        val tossesDone: Int get() = tosses.size
        val canToss: Boolean get() = !isTossing && tosses.size < TOSSES
    }

    /** [lines] are the six line values (6..9) as the server cast them. */
    data class Result(val hexagram: Hexagram, val lines: List<Int>, val firstTime: Boolean, val year: Int) : YearUi
    data object Offline : YearUi
    /** The server asked the seeker to wait before casting again. */
    data object RateLimited : YearUi
    data object Inactive : YearUi
}

/**
 * The year hexagram: opens the saved result when the account has one, otherwise runs the ritual.
 * The server casts on the first toss, so every device shows the same hexagram, and each tap then
 * reveals one of the six tosses it returned.
 */
class YearViewModel(
    private val api: OracleApi,
    private val getHexagram: GetHexagramUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val log: (String, Map<String, String>) -> Unit,
    private val campaignId: (year: Int) -> String,
    /** False when the server says the campaign is over; null while the status is unknown. */
    private val campaignActive: () -> Boolean?,
    /** Brings the Cinnabar Gold theme the server granted on the first cast. */
    private val syncStreak: suspend () -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow<YearUi>(YearUi.Loading)
    val state: StateFlow<YearUi> = _state.asStateFlow()

    val soundEnabled: StateFlow<Boolean> = observeSettings()
        .map { it.soundEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** The server's cast, once the first toss has fetched it. */
    private var cast: YearHexagram? = null
    private var busy = false

    /** Offline came from the first toss, not from opening the screen. */
    private var failedOnFirstToss = false

    init {
        load()
    }

    fun onToss() {
        val ui = _state.value as? YearUi.Ritual ?: return
        if (busy || !ui.canToss) return
        busy = true
        val index = ui.tossesDone
        _state.value = ui.copy(isTossing = true, awaitingCast = cast == null)
        viewModelScope.launch {
            try {
                val result = cast ?: castOnServer()
                val toss = CoinToss(facesFor(result, index))
                updateRitual { it.copy(currentToss = toss, tossKey = it.tossKey + 1, awaitingCast = false) }
                delay(TOSS_ANIMATION_MILLIS)
                updateRitual { it.copy(tosses = it.tosses + toss, isTossing = false) }
                if (index == TOSSES - 1) {
                    // Leave the settled coins and the sixth line on screen before moving on.
                    delay(LAST_TOSS_HOLD_MILLIS)
                    _state.value = show(result, firstTime = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                failedOnFirstToss = cast == null
                _state.value = when (e.reason) {
                    FailureReason.CAMPAIGN_INACTIVE -> YearUi.Inactive
                    FailureReason.RATE_LIMITED -> YearUi.RateLimited
                    else -> YearUi.Offline
                }
            } catch (e: Exception) {
                failedOnFirstToss = cast == null
                _state.value = YearUi.Offline
            } finally {
                busy = false
            }
        }
    }

    /** Asks the server again: by making the first toss again, or for the saved result. */
    fun retry() {
        if (busy) return
        if (failedOnFirstToss) {
            failedOnFirstToss = false
            _state.value = YearUi.Ritual()
            onToss()
        } else {
            load()
        }
    }

    /** The first-time notice about the theme has been shown. */
    fun onThemeNoticeShown() = _state.update { if (it is YearUi.Result) it.copy(firstTime = false) else it }

    private fun updateRitual(transform: (YearUi.Ritual) -> YearUi.Ritual) =
        _state.update { if (it is YearUi.Ritual) transform(it) else it }

    private suspend fun castOnServer(): YearHexagram {
        val result = api.castYearHexagram()
        cast = result
        log("year_cast", mapOf("campaign_id" to campaignId(result.year), "hexagram_id" to result.primaryId.toString()))
        // The server granted the theme with the cast; the streak sync is how it reaches this device.
        viewModelScope.launch { syncStreak() }
        return result
    }

    private fun load() {
        busy = true
        _state.value = YearUi.Loading
        viewModelScope.launch {
            _state.value = try {
                val existing = api.yearHexagram()
                when {
                    existing != null -> show(existing, firstTime = false)
                    campaignActive() == false -> YearUi.Inactive
                    else -> YearUi.Ritual()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                YearUi.Offline
            } finally {
                busy = false
            }
        }
    }

    /** Server coins as faces; a toss without coins falls back to faces that give the same line. */
    private fun facesFor(result: YearHexagram, index: Int): List<CoinFace> {
        val coins = result.coins.getOrNull(index)
        if (coins != null && coins.size == 3) return coins.map(::coinFace)
        return when (result.lines[index]) {
            6 -> listOf(CoinFace.TAILS, CoinFace.TAILS, CoinFace.TAILS)
            7 -> listOf(CoinFace.HEADS, CoinFace.TAILS, CoinFace.TAILS)
            8 -> listOf(CoinFace.HEADS, CoinFace.HEADS, CoinFace.TAILS)
            else -> listOf(CoinFace.HEADS, CoinFace.HEADS, CoinFace.HEADS)
        }
    }

    private fun coinFace(value: Int) = if (value == 3) CoinFace.HEADS else CoinFace.TAILS

    private suspend fun show(result: YearHexagram, firstTime: Boolean): YearUi.Result {
        val hexagram = getHexagram(result.primaryId)
        log("year_view", mapOf("campaign_id" to campaignId(result.year)))
        return YearUi.Result(hexagram, result.lines, firstTime, result.year)
    }
}

private val ZODIAC = listOf("ชวด", "ฉลู", "ขาล", "เถาะ", "มะโรง", "มะเส็ง", "มะเมีย", "มะแม", "วอก", "ระกา", "จอ", "กุน")

/** e.g. 2027 → "มะแม ๒๕๗๐": the zodiac animal and the Buddhist-era year. */
fun campaignYearLabel(year: Int): String = "${ZODIAC[(year - 4).mod(12)]} ${(year + 543).toThaiNumerals()}"
