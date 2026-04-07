package com.chordquiz.app.ui.screen.notemode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.ui.navigation.NoteQuizModeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteQuizModeUiState(
    val instrument: Instrument? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class NoteQuizModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val instrumentRepo: InstrumentRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<NoteQuizModeRoute>()

    private val _uiState = MutableStateFlow(NoteQuizModeUiState())
    val uiState: StateFlow<NoteQuizModeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val instrument = instrumentRepo.getInstrumentById(route.instrumentId)
            _uiState.value = NoteQuizModeUiState(instrument = instrument, isLoading = false)
        }
    }
}
