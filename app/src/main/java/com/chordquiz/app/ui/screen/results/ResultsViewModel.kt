package com.chordquiz.app.ui.screen.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.model.QuizSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Session store — in-memory singleton for passing session results between screens
object SessionStore {
    val sessions = mutableMapOf<String, QuizSession>()
    fun save(session: QuizSession) { sessions[session.id] = session }
    fun get(id: String): QuizSession? = sessions[id]
}

data class ResultsUiState(
    val session: QuizSession? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ResultsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        val session = SessionStore.get(sessionId)
        _uiState.value = ResultsUiState(session = session, isLoading = false)
    }
}
