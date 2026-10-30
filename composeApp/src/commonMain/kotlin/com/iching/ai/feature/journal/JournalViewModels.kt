package com.iching.ai.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.ObserveJournalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalEntry(val reading: Reading, val primary: Hexagram, val relating: Hexagram?)

sealed interface JournalUiState {
    data object Loading : JournalUiState
    data class Loaded(val entries: List<JournalEntry>) : JournalUiState
}

class JournalViewModel(
    observeJournal: ObserveJournalUseCase,
    private val getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
) : ViewModel() {

    val state: StateFlow<JournalUiState> = observeJournal()
        .map { readings ->
            val byId = getHexagrams().associateBy { it.id }
            JournalUiState.Loaded(
                readings.mapNotNull { r ->
                    val primary = byId[r.primaryId] ?: return@mapNotNull null
                    JournalEntry(r, primary, r.relatingId?.let(byId::get))
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Loading)

    fun delete(id: Long) {
        viewModelScope.launch { deleteReading(id) }
    }
}

class JournalEntryViewModel(
    id: Long,
    getReading: GetReadingUseCase,
    getHexagrams: GetHexagramsUseCase,
    private val deleteReading: DeleteReadingUseCase,
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

    fun delete(onDone: () -> Unit) {
        val id = _entry.value?.reading?.id ?: return
        viewModelScope.launch {
            deleteReading(id)
            onDone()
        }
    }
}
