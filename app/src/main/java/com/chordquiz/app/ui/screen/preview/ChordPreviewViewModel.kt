package com.chordquiz.app.ui.screen.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.repository.ChordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChordPreviewUiState {
    object Loading : ChordPreviewUiState()
    data class Ready(val chords: List<ChordDefinition>) : ChordPreviewUiState()
}

@HiltViewModel
class ChordPreviewViewModel @Inject constructor(
    private val chordRepo: ChordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChordPreviewUiState>(ChordPreviewUiState.Loading)
    val uiState: StateFlow<ChordPreviewUiState> = _uiState.asStateFlow()

    fun initialize(instrumentId: String, selectedChordIds: List<String>) {
        viewModelScope.launch {
            val allChords = chordRepo.getChordsForInstrument(instrumentId).first()
            val selectedIdSet = selectedChordIds.toSet()
            val filtered = allChords.filter { it.id in selectedIdSet }
            _uiState.value = ChordPreviewUiState.Ready(filtered)
        }
    }
}
