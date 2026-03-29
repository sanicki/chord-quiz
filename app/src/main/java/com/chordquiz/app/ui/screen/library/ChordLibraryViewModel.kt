package com.chordquiz.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.GetChordsForInstrumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChordLibraryUiState(
    val instrument: Instrument? = null,
    val allChords: List<ChordDefinition> = emptyList(),
    val filteredChords: List<ChordDefinition> = emptyList(),
    val selectedChordIds: Set<String> = emptySet(),
    val activeTypeFilter: ChordType? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ChordLibraryViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val getChordsForInstrument: GetChordsForInstrumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordLibraryUiState())
    val uiState: StateFlow<ChordLibraryUiState> = _uiState.asStateFlow()

    private val typeFilter = MutableStateFlow<ChordType?>(null)

    fun loadInstrument(instrumentId: String) {
        viewModelScope.launch {
            val instrument = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            combine(
                getChordsForInstrument(instrumentId),
                typeFilter
            ) { chords, filter ->
                val filtered = if (filter == null) chords else chords.filter { it.chordType == filter }
                _uiState.value = _uiState.value.copy(
                    instrument = instrument,
                    allChords = chords,
                    filteredChords = filtered,
                    activeTypeFilter = filter,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun toggleChordSelection(chordId: String) {
        val current = _uiState.value.selectedChordIds.toMutableSet()
        if (current.contains(chordId)) current.remove(chordId) else current.add(chordId)
        _uiState.value = _uiState.value.copy(selectedChordIds = current)
    }

    fun setTypeFilter(type: ChordType?) {
        typeFilter.value = type
        val filtered = if (type == null) _uiState.value.allChords
        else _uiState.value.allChords.filter { it.chordType == type }
        _uiState.value = _uiState.value.copy(
            filteredChords = filtered,
            activeTypeFilter = type
        )
    }

    fun selectAll() {
        val ids = _uiState.value.filteredChords.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedChordIds = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedChordIds = emptySet())
    }
}
