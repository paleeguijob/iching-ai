package com.iching.ai.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.SearchHexagramsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val query: String = "",
    val hexagrams: List<Hexagram> = emptyList(),
    val isLoading: Boolean = true,
    /** Trigram chip; null shows every hexagram. */
    val trigram: Trigram? = null,
)

/** The search results narrowed to the selected trigram chip. */
val LibraryUiState.visible: List<Hexagram> get() = filterByTrigram(hexagrams, trigram)

/** Hexagrams that hold [trigram] as either their upper or lower half; all of them when it is null. */
internal fun filterByTrigram(hexagrams: List<Hexagram>, trigram: Trigram?): List<Hexagram> =
    if (trigram == null) hexagrams else hexagrams.filter { it.upperTrigram == trigram || it.lowerTrigram == trigram }

class LibraryViewModel(private val search: SearchHexagramsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onTrigramChange(trigram: Trigram?) {
        _state.update { it.copy(trigram = trigram) }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        refresh()
    }

    private fun refresh() {
        val query = _state.value.query
        viewModelScope.launch {
            val result = search(query)
            if (_state.value.query == query) _state.update { it.copy(hexagrams = result, isLoading = false) }
        }
    }
}

class HexagramDetailViewModel(id: Int, getHexagram: GetHexagramUseCase) : ViewModel() {
    private val _hexagram = MutableStateFlow<Hexagram?>(null)
    val hexagram: StateFlow<Hexagram?> = _hexagram.asStateFlow()

    init {
        viewModelScope.launch { _hexagram.value = getHexagram(id) }
    }
}
