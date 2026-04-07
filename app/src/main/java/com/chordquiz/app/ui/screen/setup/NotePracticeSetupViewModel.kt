package com.chordquiz.app.ui.screen.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.ui.navigation.NotePracticeSetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class NotePracticeSetupUiState(
    val noteMode: NoteMode,
    val mode: QuizMode = QuizMode.NOTE_DRAW,
    val questionCount: Int = 10,
    val repeatMissed: Boolean = true
)

@HiltViewModel
class NotePracticeSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<NotePracticeSetupRoute>()

    private val _uiState = MutableStateFlow(
        NotePracticeSetupUiState(
            noteMode = route.noteMode,
            repeatMissed = route.initialRepeatMissed
        )
    )
    val uiState: StateFlow<NotePracticeSetupUiState> = _uiState.asStateFlow()

    fun setMode(mode: QuizMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun incrementQuestionCount() {
        val cur = _uiState.value.questionCount
        _uiState.value = _uiState.value.copy(questionCount = cur + 5)
    }

    fun decrementQuestionCount() {
        val cur = _uiState.value.questionCount
        if (cur > 5) {
            _uiState.value = _uiState.value.copy(questionCount = cur - 5)
        }
    }

    fun setRepeatMissed(repeat: Boolean) {
        _uiState.value = _uiState.value.copy(repeatMissed = repeat)
    }
}
