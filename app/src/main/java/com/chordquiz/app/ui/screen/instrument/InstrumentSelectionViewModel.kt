package com.chordquiz.app.ui.screen.instrument

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.preferences.UserPreferencesRepository
import com.chordquiz.app.domain.GetInstrumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuizType { CHORD, NOTE }

data class InstrumentSelectionUiState(
    val instruments: List<Instrument> = emptyList(),
    val isLoading: Boolean = true,
    val quizType: QuizType = QuizType.CHORD
)

@HiltViewModel
class InstrumentSelectionViewModel @Inject constructor(
    private val getInstruments: GetInstrumentsUseCase,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstrumentSelectionUiState())
    val uiState: StateFlow<InstrumentSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getInstruments().collect { instruments ->
                _uiState.value = _uiState.value.copy(
                    instruments = instruments,
                    isLoading = false
                )
            }
        }
    }

    fun onQuizTypeChanged(type: QuizType) {
        _uiState.value = _uiState.value.copy(quizType = type)
    }

    fun onInstrumentSelected(instrument: Instrument) {
        viewModelScope.launch {
            prefsRepo.setLastInstrumentId(instrument.id)
        }
    }
}
