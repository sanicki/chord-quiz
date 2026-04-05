package com.chordquiz.app.ui.screen.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.ui.navigation.PracticeSetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.navigation.toRoute
import javax.inject.Inject

data class PracticeSetupUiState(
    val mode: QuizMode = QuizMode.DRAW,
    val questionCount: Int,
    val repeatMissed: Boolean = true
)

@HiltViewModel
class PracticeSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chordCount =
        savedStateHandle.toRoute<PracticeSetupRoute>().selectedChordIds.size

    private val _uiState = MutableStateFlow(
        PracticeSetupUiState(questionCount = computeDefaultQuestionCount(chordCount))
    )
    val uiState: StateFlow<PracticeSetupUiState> = _uiState.asStateFlow()

    fun setMode(mode: QuizMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun incrementQuestionCount() {
        _uiState.value = _uiState.value.copy(questionCount = _uiState.value.questionCount + 1)
    }

    fun decrementQuestionCount() {
        val cur = _uiState.value.questionCount
        if (cur > chordCount.coerceAtLeast(1)) {
            _uiState.value = _uiState.value.copy(questionCount = cur - 1)
        }
    }

    fun setRepeatMissed(repeat: Boolean) {
        _uiState.value = _uiState.value.copy(repeatMissed = repeat)
    }

    companion object {
        fun computeDefaultQuestionCount(chordCount: Int): Int {
            val raw = chordCount.coerceAtLeast(1) * 3
            return ((raw + 2) / 5) * 5
        }
    }
}
