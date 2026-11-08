package com.iching.ai.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.ui.toThaiNumerals
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.FilterJournalUseCase
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.ObserveJournalUseCase
import com.iching.ai.domain.usecase.SaveOutcomeUseCase
import com.iching.ai.session.AppSession
import com.iching.ai.ui.formatThaiMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** A journal row: an I Ching cast has its [primary] hexagram; a Siam Si draw has [siamsi] instead. */
data class JournalEntry(val reading: Reading, val primary: Hexagram?, val relating: Hexagram?, val siamsi: SiamSiJournal? = null)

/** A Siam Si reading in the journal. [set] and [stick] are null when that set is no longer on the device. */
data class SiamSiJournal(val pick: SiamSiPick, val set: SiamSiSet?, val stick: SiamSiStick?, val label: String)

enum class JournalFilter(val labelTh: String) { ALL("ทั้งหมด"), ICHING("อี้จิง"), SIAMSI("เซียมซี") }

fun filterJournal(readings: List<Reading>, filter: JournalFilter): List<Reading> = when (filter) {
    JournalFilter.ALL -> readings
    JournalFilter.ICHING -> readings.filter { it.siamsi == null }
    JournalFilter.SIAMSI -> readings.filter { it.siamsi != null }
}

/** "เซียมซีเจ้าแม่กวนอิม" → "กวนอิม". */
fun shortSetName(name: String): String = name.removePrefix("เซียมซี").removePrefix("เจ้าแม่").trim()

/** "กวนอิม ใบที่ ๑๗ · ดี · มีคำแปล"; [level] is null when the slip itself is no longer on the device. */
fun siamsiRowLabel(setName: String, number: Int, level: SiamSiLevel?, labelOrig: String?, hasInterpretation: Boolean): String =
    buildString {
        append("${shortSetName(setName)} ใบที่ ${number.toThaiNumerals()}")
        labelOrig?.let { append(" $it") }
        level?.let { append(" · ${it.labelTh}") }
        if (hasInterpretation) append(" · มีคำแปล")
    }

/** The row for [reading], or null for a cast whose hexagram cannot be found. */
fun journalEntry(reading: Reading, hexagrams: Map<Int, Hexagram>, sets: Map<String, SiamSiSet>): JournalEntry? {
    val pick = reading.siamsi
    if (pick != null) {
        val set = sets[pick.setId]
        val stick = set?.sticks?.firstOrNull { it.number == pick.number }
        val label = siamsiRowLabel(set?.name ?: pick.setId, pick.number, stick?.level, stick?.labelOrig, reading.interpretation.isNotBlank())
        return JournalEntry(reading, primary = null, relating = null, siamsi = SiamSiJournal(pick, set, stick, label))
    }
    val primary = hexagrams[reading.primaryId] ?: return null
    return JournalEntry(reading, primary, reading.relatingId?.let(hexagrams::get))
}

/** The sets the journal's Siam Si readings come from, loaded once each; missing ones are left out. */
private suspend fun setsFor(readings: List<Reading>, repository: SiamSiRepository): Map<String, SiamSiSet> =
    readings.mapNotNull { it.siamsi?.setId }.distinct().mapNotNull { repository.set(it) }.associateBy { it.id }

/** One month of the timeline, newest first, with its heading. */
data class JournalMonth(val label: String, val entries: List<JournalEntry>)

sealed interface JournalUiState {
    data object Loading : JournalUiState

    /** [hiddenCount] older readings are kept on the device but shown only to Premium members. */
    /** [hasSiamSi]: the filter chips show once there is a Siam Si reading to filter by. */
    data class Loaded(val months: List<JournalMonth>, val hiddenCount: Int, val journalDays: Int?, val hasSiamSi: Boolean = false) : JournalUiState {
        val isEmpty: Boolean get() = months.isEmpty() && hiddenCount == 0
        val entryCount: Int get() = months.sumOf { it.entries.size }
    }
}

class JournalViewModel(
    observeJournal: ObserveJournalUseCase,
    private val getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
    private val filterJournal: FilterJournalUseCase,
    session: AppSession,
    private val siamsiSets: SiamSiRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(JournalFilter.ALL)
    val filter: StateFlow<JournalFilter> = _filter.asStateFlow()

    val state: StateFlow<JournalUiState> = combine(observeJournal(), session.status, _filter) { readings, status, filter ->
        val byId = getHexagrams().associateBy { it.id }
        val sets = setsFor(readings, siamsiSets)
        val (visible, hidden) = filterJournal(readings, status.journalDays, Clock.System.now().toEpochMilliseconds())
        val entries = filterJournal(visible, filter).mapNotNull { journalEntry(it, byId, sets) }
        JournalUiState.Loaded(
            months = entries.groupBy { formatThaiMonth(it.reading.createdAtMillis) }.map { (label, list) -> JournalMonth(label, list) },
            hiddenCount = hidden,
            journalDays = status.journalDays,
            hasSiamSi = readings.any { it.siamsi != null },
        )
    }
        // Decoding and grouping every reading must not hold up the frame that opens this tab.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Loading)

    fun onFilterSelected(filter: JournalFilter) {
        _filter.value = filter
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteReading(id) }
    }
}

class JournalEntryViewModel(
    id: Long,
    getReading: GetReadingUseCase,
    getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
    private val saveOutcome: SaveOutcomeUseCase,
    private val siamsiSets: SiamSiRepository,
    private val log: (String, Map<String, String>) -> Unit = { _, _ -> },
) : ViewModel() {
    private val _entry = MutableStateFlow<JournalEntry?>(null)
    val entry: StateFlow<JournalEntry?> = _entry.asStateFlow()

    init {
        viewModelScope.launch {
            val reading = getReading(id) ?: return@launch
            val byId = getHexagrams().associateBy { it.id }
            _entry.value = journalEntry(reading, byId, setsFor(listOf(reading), siamsiSets))
        }
    }

    /** "Did it come true?" — saved on the device and, when the reading came from the server, there too. */
    fun setOutcome(outcome: Outcome?, note: String?) {
        val current = _entry.value ?: return
        val updated = current.reading.copy(outcome = outcome, outcomeNote = note?.trim()?.ifEmpty { null })
        _entry.value = current.copy(reading = updated)
        viewModelScope.launch { saveOutcome(updated) }
    }

    fun logEvent(name: String, params: Map<String, String>) = log(name, params)

    fun delete(onDone: () -> Unit) {
        val id = _entry.value?.reading?.id ?: return
        viewModelScope.launch {
            deleteReading(id)
            onDone()
        }
    }
}
