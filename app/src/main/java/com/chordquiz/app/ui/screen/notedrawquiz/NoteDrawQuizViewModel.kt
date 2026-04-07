package com.chordquiz.app.ui.screen.notedrawquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizAnswer
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.BuildNoteQuizSessionUseCase
import com.chordquiz.app.domain.EvaluateNoteDrawAnswerUseCase
import com.chordquiz.app.haptic.HapticManager
import com.chordquiz.app.ui.shared.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NoteDrawQuizUiState {
    object Loading : NoteDrawQuizUiState()
    data class Active(
        val session: QuizSession,
        val currentFingering: Fingering,
        val feedback: NoteDrawFeedback? = null,
        val missedPositions: Set<Int> = emptySet(),
        val displayedQuestion: QuizQuestion? = null,
        val displayedQuestionIndex: Int = 0
    ) : NoteDrawQuizUiState()
    data class Complete(val sessionId: String) : NoteDrawQuizUiState()
}

enum class NoteDrawFeedback { CORRECT, INCORRECT }

@HiltViewModel
class NoteDrawQuizViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val buildNoteSession: BuildNoteQuizSessionUseCase,
    private val evaluateNoteDrawAnswer: EvaluateNoteDrawAnswerUseCase,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDrawQuizUiState>(NoteDrawQuizUiState.Loading)
    val uiState: StateFlow<NoteDrawQuizUiState> = _uiState.asStateFlow()

    private var instrument: Instrument? = null

    fun initialize(
        instrumentId: String,
        noteMode: NoteMode,
        questionCount: Int,
        repeatMissed: Boolean
    ) {
        viewModelScope.launch {
            val inst = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            instrument = inst
            val session = buildNoteSession.buildDrawSession(inst, noteMode, questionCount, repeatMissed)
            val firstQuestion = session.questions.firstOrNull()
            _uiState.value = NoteDrawQuizUiState.Active(
                session = session,
                currentFingering = emptyFingering(inst.stringCount),
                displayedQuestion = firstQuestion,
                displayedQuestionIndex = 0
            )
        }
    }

    fun onFingeringChanged(fingering: Fingering) {
        val state = _uiState.value as? NoteDrawQuizUiState.Active ?: return
        if (state.feedback != null) return
        _uiState.value = state.copy(
            currentFingering = fingering,
            feedback = null,
            missedPositions = emptySet()
        )
    }

    fun submitAnswer() {
        val state = _uiState.value as? NoteDrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        val question = state.displayedQuestion ?: state.session.currentQuestion ?: return
        val noteQuestion = question as? QuizQuestion.NoteQuestion ?: return

        val result = evaluateNoteDrawAnswer(inst, state.currentFingering, noteQuestion)

        val answer = QuizAnswer(
            question = noteQuestion,
            isCorrect = result.isCorrect,
            userFingering = state.currentFingering
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = if (result.isCorrect) NoteDrawFeedback.CORRECT else NoteDrawFeedback.INCORRECT,
            missedPositions = result.missedPositions.map { it.stringIndex }.toSet()
        )

        if (!result.isCorrect) {
            viewModelScope.launch {
                hapticManager.vibrateWrongAnswer()
            }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? NoteDrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        if (state.session.isComplete) {
            SessionStore.save(state.session)
            _uiState.value = NoteDrawQuizUiState.Complete(state.session.id)
        } else {
            val nextQuestion = state.session.currentQuestion
            _uiState.value = state.copy(
                currentFingering = emptyFingering(inst.stringCount),
                feedback = null,
                missedPositions = emptySet(),
                displayedQuestion = nextQuestion,
                displayedQuestionIndex = state.displayedQuestionIndex + 1
            )
        }
    }

    private fun emptyFingering(stringCount: Int) = Fingering(
        positions = (0 until stringCount).map { StringPosition(it, 0) }
    )
}
