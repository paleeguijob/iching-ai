package com.iching.ai.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.FilterJournalUseCase
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.ObserveJournalUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class JournalEntry(val reading: Reading, val primary: Hexagram, val relating: Hexagram?)

sealed interface JournalUiState {
    data object Loading : JournalUiState

    /** [hiddenCount] older readings are kept on the device but shown only to Premium members. */
    data class Loaded(val entries: List<JournalEntry>, val hiddenCount: Int, val journalDays: Int?) : JournalUiState
}

class JournalViewModel(
    observeJournal: ObserveJournalUseCase,
    private val getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
    private val filterJournal: FilterJournalUseCase,
    session: AppSession,
) : ViewModel() {

    val state: StateFlow<JournalUiState> = combine(observeJournal(), session.status) { readings, status ->
        val byId = getHexagrams().associateBy { it.id }
        val (visible, hidden) = filterJournal(readings, status.journalDays, Clock.System.now().toEpochMilliseconds())
        JournalUiState.Loaded(
            entries = visible.mapNotNull { r ->
                val primary = byId[r.primaryId] ?: return@mapNotNull null
                JournalEntry(r, primary, r.relatingId?.let(byId::get))
            },
            hiddenCount = hidden,
            journalDays = status.journalDays,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Loading)

    fun delete(id: Long) {
        viewModelScope.launch { deleteReading(id) }
    }
}

class JournalEntryViewModel(
    id: Long,
    getReading: GetReadingUseCase,
    getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
    private val saveReading: SaveReadingUseCase,
    private val api: OracleApi,
) : ViewModel() {
    private val _entry = MutableStateFlow<JournalEntry?>(null)
    val entry: StateFlow<JournalEntry?> = _entry.asStateFlow()

    init {
        viewModelScope.launch {
            val reading = getReading(id) ?: return@launch
            val byId = getHexagrams().associateBy { it.id }
            val primary = byId[reading.primaryId] ?: return@launch
            _entry.value = JournalEntry(reading, primary, reading.relatingId?.let(byId::get))
        }
    }

    /** "Did it come true?" — saved on the device and, when the reading came from the server, there too. */
    fun setOutcome(outcome: Outcome?, note: String?) {
        val current = _entry.value ?: return
        val updated = current.reading.copy(outcome = outcome, outcomeNote = note?.trim()?.ifEmpty { null })
        _entry.value = current.copy(reading = updated)
        viewModelScope.launch {
            saveReading(updated)
            updated.serverId?.let { serverId -> runCatching { api.saveOutcome(serverId, updated.outcome, updated.outcomeNote) } }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _entry.value?.reading?.id ?: return
        viewModelScope.launch {
            deleteReading(id)
            onDone()
        }
    }
}
