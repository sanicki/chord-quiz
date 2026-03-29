package com.chordquiz.app.ui.screen.setup

import androidx.lifecycle.ViewModel
import com.chordquiz.app.data.model.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PracticeSetupUiState(
    val mode: QuizMode = QuizMode.DRAW,
    val questionCount: Int = 10,
    val repeatMissed: Boolean = true
)

@HiltViewModel
class PracticeSetupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeSetupUiState())
    val uiState: StateFlow<PracticeSetupUiState> = _uiState.asStateFlow()

    fun setMode(mode: QuizMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun incrementQuestionCount() {
        val cur = _uiState.value.questionCount
        if (cur < 30) _uiState.value = _uiState.value.copy(questionCount = cur + 1)
    }

    fun decrementQuestionCount() {
        val cur = _uiState.value.questionCount
        if (cur > 3) _uiState.value = _uiState.value.copy(questionCount = cur - 1)
    }

    fun setRepeatMissed(repeat: Boolean) {
        _uiState.value = _uiState.value.copy(repeatMissed = repeat)
    }
}
