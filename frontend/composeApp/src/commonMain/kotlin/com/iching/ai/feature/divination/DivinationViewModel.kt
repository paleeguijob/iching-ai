package com.iching.ai.feature.divination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.FollowUpTurn
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.oracle.ScriptureReadingComposer
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.LimitReason
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.session.AdResult
import com.iching.ai.session.AppSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock

enum class DivinationPhase { CASTING, REVEAL, INTERPRETING, COMPLETE, SAFETY }

/** Why the scroll shows raw scripture instead of the hermit's words. */
sealed interface ReadingNotice {
    data class Limited(val reason: LimitReason, val retryAfterSeconds: Int?) : ReadingNotice
    data class Failed(val reason: FailureReason) : ReadingNotice

    /** The server could not be reached; the coins were cast on this device. */
    data object Offline : ReadingNotice
}

data class FollowUpUi(val question: String, val answer: String, val streaming: Boolean)

data class DivinationUiState(
    val question: String,
    val category: QuestionCategory? = null,
    val tosses: List<CoinToss> = emptyList(),
    /** The toss currently in the air; drives the coin animation. */
    val currentToss: CoinToss? = null,
    val tossKey: Int = 0,
    val isTossing: Boolean = false,
    /** Waiting for the server to cast (first toss only). */
    val awaitingCast: Boolean = false,
    val phase: DivinationPhase = DivinationPhase.CASTING,
    val cast: CastResult? = null,
    val interpretation: String = "",
    val shareSummary: String? = null,
    val source: InterpretationSource? = null,
    val notice: ReadingNotice? = null,
    val serverReadingId: String? = null,
    val savedReadingId: Long? = null,
    val createdAtMillis: Long = 0,
    val soundEnabled: Boolean = true,
    val safetyMessage: String? = null,
    val status: AccountStatus = AccountStatus.UNKNOWN_FREE,
    val isWatchingAd: Boolean = false,
    val message: String? = null,
    val followUps: List<FollowUpUi> = emptyList(),
    val followUpBusy: Boolean = false,
    val reported: Boolean = false,
    val error: String? = null,
) {
    val tossesDone: Int get() = tosses.size
    val canToss: Boolean get() = phase == DivinationPhase.CASTING && !isTossing && tosses.size < 6
    val isAiReading: Boolean get() = source == InterpretationSource.AI && serverReadingId != null
    val followUpsLeft: Int get() = (status.followUpsPerReading - followUps.count { !it.streaming }).coerceAtLeast(0)
}

private sealed interface CastSource {
    data class Server(val event: DivinationEvent.Cast) : CastSource
    data class Local(val tosses: List<CoinToss>, val reason: ReadingNotice) : CastSource
    data class Safety(val message: String) : CastSource
}

private sealed interface StreamOutcome {
    data object Done : StreamOutcome
    data class Scripture(val notice: ReadingNotice) : StreamOutcome
}

class DivinationViewModel(
    question: String,
    category: QuestionCategory?,
    private val tossCoins: TossCoinsUseCase,
    private val resolveCast: ResolveCastUseCase,
    private val saveReading: SaveReadingUseCase,
    private val api: OracleApi,
    private val session: AppSession,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DivinationUiState(question = question.trim(), category = category))
    val state: StateFlow<DivinationUiState> = _state.asStateFlow()

    private val castSource = CompletableDeferred<CastSource>()
    private var requestJob: Job? = null
    private var autoTossJob: Job? = null

    /** Model text received so far; shown once the hexagram has been revealed. */
    private var streamText = StringBuilder()
    private var outcome: StreamOutcome? = null

    init {
        viewModelScope.launch {
            observeSettings().collect { s -> _state.update { it.copy(soundEnabled = s.soundEnabled) } }
        }
        viewModelScope.launch {
            session.status.collect { s -> _state.update { it.copy(status = s) } }
        }
        session.log("reading_started", mapOf("category" to (category?.wire ?: "none")))
    }

    // ---------- casting ----------

    fun onToss() {
        if (!_state.value.canToss) return
        if (requestJob == null) startDivination()
        val index = _state.value.tosses.size
        _state.update { it.copy(isTossing = true, awaitingCast = index == 0) }
        viewModelScope.launch {
            val source = withTimeoutOrNull(CAST_TIMEOUT_MILLIS) { castSource.await() } ?: fallBackToLocalCast(ReadingNotice.Offline)
            if (source is CastSource.Safety) {
                _state.update { it.copy(isTossing = false, awaitingCast = false, phase = DivinationPhase.SAFETY, safetyMessage = source.message) }
                return@launch
            }
            val toss = when (source) {
                is CastSource.Server -> CoinToss(source.event.coins.getOrNull(index) ?: facesFor(source.event.lines[index].value))
                is CastSource.Local -> source.tosses[index]
                is CastSource.Safety -> return@launch
            }
            _state.update { it.copy(currentToss = toss, tossKey = it.tossKey + 1, awaitingCast = false) }
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
                delay(250)
            }
        }
    }

    private fun startDivination() {
        val s = _state.value
        requestJob = viewModelScope.launch { consume(api.divine(s.question, s.category), fromCast = true) }
    }

    private suspend fun fallBackToLocalCast(reason: ReadingNotice): CastSource {
        val local = CastSource.Local(List(6) { tossCoins() }, reason)
        if (castSource.complete(local)) {
            outcome = StreamOutcome.Scripture(reason)
            requestJob?.cancel()
        }
        // Already completed one way or the other, so this returns at once.
        return castSource.await()
    }

    /** Server lines are authoritative; coin faces are only for the animation. */
    private fun facesFor(lineValue: Int): List<CoinFace> = when (lineValue) {
        6 -> listOf(CoinFace.TAILS, CoinFace.TAILS, CoinFace.TAILS)
        7 -> listOf(CoinFace.HEADS, CoinFace.TAILS, CoinFace.TAILS)
        8 -> listOf(CoinFace.HEADS, CoinFace.HEADS, CoinFace.TAILS)
        else -> listOf(CoinFace.HEADS, CoinFace.HEADS, CoinFace.HEADS)
    }

    private suspend fun finishCast() {
        try {
            val source = castSource.await()
            val lines = when (source) {
                is CastSource.Server -> source.event.lines
                is CastSource.Local -> source.tosses.map { it.line }
                is CastSource.Safety -> return
            }
            val cast = resolveCast(lines)
            _state.update {
                it.copy(
                    cast = cast,
                    phase = DivinationPhase.REVEAL,
                    serverReadingId = (source as? CastSource.Server)?.event?.readingId,
                    createdAtMillis = Clock.System.now().toEpochMilliseconds(),
                )
            }
            delay(REVEAL_MILLIS)
            _state.update { it.copy(phase = DivinationPhase.INTERPRETING, interpretation = streamText.toString()) }
            settleIfReady()
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "ไม่สามารถเปิดคัมภีร์ได้") }
        }
    }

    // ---------- streaming ----------

    private suspend fun consume(events: Flow<DivinationEvent>, fromCast: Boolean) {
        var sawCast = !fromCast
        events.collect { event ->
            when (event) {
                is DivinationEvent.Cast -> {
                    sawCast = true
                    castSource.complete(CastSource.Server(event))
                }
                is DivinationEvent.Delta -> {
                    streamText.append(event.text)
                    if (_state.value.phase == DivinationPhase.INTERPRETING) {
                        _state.update { it.copy(interpretation = streamText.toString()) }
                    }
                }
                is DivinationEvent.Done -> {
                    session.updateStatus(event.status)
                    _state.update { it.copy(shareSummary = event.shareSummary) }
                    outcome = StreamOutcome.Done
                    settleIfReady()
                }
                is DivinationEvent.Limited -> {
                    session.updateStatus(event.status)
                    outcome = StreamOutcome.Scripture(ReadingNotice.Limited(event.reason, event.retryAfterSeconds))
                    settleIfReady()
                }
                is DivinationEvent.Safety -> castSource.complete(CastSource.Safety(event.message))
                is DivinationEvent.Failed -> {
                    if (!sawCast) {
                        // Never reached the server's cast: divine on this device instead.
                        fallBackToLocalCast(if (event.reason == FailureReason.NETWORK) ReadingNotice.Offline else ReadingNotice.Failed(event.reason))
                    } else {
                        outcome = StreamOutcome.Scripture(ReadingNotice.Failed(event.reason))
                        settleIfReady()
                    }
                }
            }
        }
        if (outcome == null && sawCast) {
            outcome = StreamOutcome.Scripture(ReadingNotice.Failed(FailureReason.NETWORK))
            settleIfReady()
        }
    }

    /** Completes the reading once both the reveal animation and the stream have finished. */
    private suspend fun settleIfReady() {
        val s = _state.value
        val result = outcome ?: return
        val cast = s.cast ?: return
        if (s.phase != DivinationPhase.INTERPRETING) return
        when (result) {
            StreamOutcome.Done -> _state.update {
                it.copy(interpretation = streamText.toString().trim(), source = InterpretationSource.AI, notice = null, phase = DivinationPhase.COMPLETE)
            }
            is StreamOutcome.Scripture -> _state.update {
                it.copy(
                    interpretation = ScriptureReadingComposer.compose(cast),
                    source = InterpretationSource.SCRIPTURE,
                    notice = result.notice,
                    phase = DivinationPhase.COMPLETE,
                )
            }
        }
        session.log("reading_completed", mapOf("source" to (_state.value.source?.name ?: "")))
        persist()
    }

    /** Asks the server to interpret the same cast again (after an ad, a purchase or a failure). */
    fun onInterpretAgain() {
        val id = _state.value.serverReadingId ?: return
        if (_state.value.phase != DivinationPhase.COMPLETE || requestJob?.isActive == true) return
        streamText = StringBuilder()
        outcome = null
        _state.update { it.copy(phase = DivinationPhase.INTERPRETING, interpretation = "", source = null, notice = null, message = null) }
        requestJob = viewModelScope.launch { consume(api.interpretAgain(id), fromCast = false) }
    }

    fun onWatchAd() {
        if (_state.value.isWatchingAd) return
        _state.update { it.copy(isWatchingAd = true, message = null) }
        viewModelScope.launch {
            val result = session.watchAdForReading()
            _state.update { it.copy(isWatchingAd = false) }
            when (result) {
                is AdResult.Granted -> onInterpretAgain()
                AdResult.NotCompleted -> _state.update { it.copy(message = "ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์") }
                AdResult.LimitReached -> _state.update { it.copy(message = "วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว") }
                is AdResult.Error -> _state.update { it.copy(message = "ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่") }
            }
        }
    }

    /** Called when the seeker returns from the paywall: if they bought something, unlock this reading. */
    fun onReturnedFromPaywall() {
        val s = _state.value
        if (s.notice is ReadingNotice.Limited && s.status.readingsLeft > 0) onInterpretAgain()
    }

    // ---------- follow-up (Premium) ----------

    fun onSendFollowUp(message: String) {
        val s = _state.value
        val id = s.serverReadingId ?: return
        val text = message.trim().take(300)
        if (text.isEmpty() || s.followUpBusy || s.followUpsLeft <= 0) return
        _state.update { it.copy(followUpBusy = true, followUps = it.followUps + FollowUpUi(text, "", streaming = true), message = null) }
        session.log("follow_up_sent")
        viewModelScope.launch {
            var answer = StringBuilder()
            fun setAnswer(value: String, streaming: Boolean) = _state.update {
                it.copy(followUps = it.followUps.dropLast(1) + FollowUpUi(text, value, streaming))
            }
            api.followUp(id, text).collect { event ->
                when (event) {
                    is DivinationEvent.Delta -> {
                        answer.append(event.text)
                        setAnswer(answer.toString(), true)
                    }
                    is DivinationEvent.Done -> setAnswer(answer.toString().trim(), false)
                    is DivinationEvent.Safety -> {
                        answer = StringBuilder(event.message)
                        setAnswer(event.message, false)
                    }
                    is DivinationEvent.Limited -> {
                        _state.update { it.copy(followUps = it.followUps.dropLast(1), message = "ถามต่อครบจำนวนครั้งของคำทำนายนี้แล้ว") }
                    }
                    is DivinationEvent.Failed -> {
                        _state.update { it.copy(followUps = it.followUps.dropLast(1), message = followUpError(event.reason)) }
                    }
                    is DivinationEvent.Cast -> Unit
                }
            }
            // A stream that ended without "done" keeps whatever arrived.
            _state.update { st ->
                st.copy(followUpBusy = false, followUps = st.followUps.map { if (it.streaming) it.copy(streaming = false) else it })
            }
            persist()
        }
    }

    private fun followUpError(reason: FailureReason) = when (reason) {
        FailureReason.PREMIUM_REQUIRED -> "การถามต่อเป็นสิทธิ์ของสมาชิก Premium"
        FailureReason.NETWORK -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
        else -> "นักพรตยังไม่อาจตอบได้ในตอนนี้ โปรดลองใหม่อีกครั้ง"
    }

    // ---------- report ----------

    fun onReport(reason: String) {
        val id = _state.value.serverReadingId ?: return
        viewModelScope.launch {
            val ok = runCatching { api.report(id, reason, "") }.isSuccess
            _state.update {
                it.copy(reported = ok, message = if (ok) "ขอบคุณ เราได้รับรายงานแล้วและจะตรวจสอบ" else "ส่งรายงานไม่สำเร็จ โปรดลองใหม่")
            }
            if (ok) session.log("report_sent", mapOf("reason" to reason))
        }
    }

    fun logEvent(name: String, params: Map<String, String>) = session.log(name, params)

    fun onMessageShown() = _state.update { it.copy(message = null) }

    /** Writes the reading to the Destiny Journal; later changes update the same entry. */
    private suspend fun persist() {
        val s = _state.value
        val cast = s.cast ?: return
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
                serverId = s.serverReadingId,
                category = s.category,
                followUps = s.followUps.filter { !it.streaming && it.answer.isNotBlank() }.map { FollowUpTurn(it.question, it.answer) },
            )
        )
        _state.update { it.copy(savedReadingId = id) }
    }

    companion object {
        const val TOSS_ANIMATION_MILLIS = 1350L
        const val REVEAL_MILLIS = 2200L
        const val CAST_TIMEOUT_MILLIS = 12_000L
    }
}
