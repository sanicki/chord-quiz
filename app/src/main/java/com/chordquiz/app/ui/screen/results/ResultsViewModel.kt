package com.chordquiz.app.ui.screen.results

import androidx.lifecycle.ViewModel
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.ui.shared.SessionStore
import com.chordquiz.app.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ResultsUiState(
    val session: QuizSession? = null,
    val isLoading: Boolean = true,
    val formattedDuration: String = ""
)

@HiltViewModel
class ResultsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        val session = SessionStore.get(sessionId)
        _uiState.value = ResultsUiState(
            session = session,
            isLoading = false,
            formattedDuration = if (session != null) formatDuration(session.totalDurationMillis) else ""
        )
    }
}
