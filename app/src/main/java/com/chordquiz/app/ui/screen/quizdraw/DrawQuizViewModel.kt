package com.chordquiz.app.ui.screen.quizdraw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.NoteFrequencyTable
import com.chordquiz.app.audio.NotePlayer
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.QuizAnswer
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.data.repository.ChordRepository
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.BuildQuizSessionUseCase
import com.chordquiz.app.domain.EvaluateDrawAnswerUseCase
import com.chordquiz.app.ui.screen.results.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DrawQuizUiState {
    object Loading : DrawQuizUiState()
    data class Active(
        val session: QuizSession,
        val currentFingering: Fingering,
        val feedback: AnswerFeedback? = null,
        /** The question currently shown on screen; does not advance until Next is pressed. */
        val displayedQuestion: QuizQuestion? = null,
        /** Increments each time the user moves to the next question; used to reset the diagram. */
        val displayedQuestionIndex: Int = 0
    ) : DrawQuizUiState()
    data class Complete(val sessionId: String) : DrawQuizUiState()
}

enum class AnswerFeedback { CORRECT, INCORRECT }

@HiltViewModel
class DrawQuizViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val chordRepo: ChordRepository,
    private val buildSession: BuildQuizSessionUseCase,
    private val evaluateAnswer: EvaluateDrawAnswerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrawQuizUiState>(DrawQuizUiState.Loading)
    val uiState: StateFlow<DrawQuizUiState> = _uiState.asStateFlow()

    private var instrument: Instrument? = null

    fun initialize(
        instrumentId: String,
        selectedChordIds: List<String>,
        questionCount: Int,
        repeatMissed: Boolean
    ) {
        viewModelScope.launch {
            val inst = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            instrument = inst
            val allChords = chordRepo.getChordsForInstrument(instrumentId).first()
            val selected = allChords.filter { it.id in selectedChordIds }
            if (selected.isEmpty()) return@launch

            val session = buildSession(inst, QuizMode.DRAW, selected, questionCount, repeatMissed)
            _uiState.value = DrawQuizUiState.Active(
                session = session,
                currentFingering = emptyFingering(inst.stringCount),
                displayedQuestion = session.questions.firstOrNull(),
                displayedQuestionIndex = 0
            )
        }
    }

    fun onFingeringChanged(fingering: Fingering) {
        val state = _uiState.value as? DrawQuizUiState.Active ?: return
        _uiState.value = state.copy(currentFingering = fingering, feedback = null)
    }

    fun onNoteSelected(stringIndex: Int, fret: Int) {
        val inst = instrument ?: return
        if (fret < 0) return
        val openNote = inst.openStringNotes.getOrNull(stringIndex) ?: return
        val openOctave = inst.openStringOctaves.getOrNull(stringIndex) ?: return
        val midi = openNote.semitone + 12 * (openOctave + 1) + fret
        viewModelScope.launch {
            NotePlayer.playMidi(midi)
        }
    }

    fun submitAnswer() {
        val state = _uiState.value as? DrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        val question = state.displayedQuestion ?: state.session.currentQuestion ?: return

        val referenceFingering = question.chordDefinition.fingerings.getOrNull(question.targetFingeringIndex)
        val isCorrect = evaluateAnswer(inst, state.currentFingering, question.chordDefinition, referenceFingering)
        val answer = QuizAnswer(
            question = question,
            isCorrect = isCorrect,
            userFingering = state.currentFingering
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = if (isCorrect) AnswerFeedback.CORRECT else AnswerFeedback.INCORRECT
        )
    }

    fun nextQuestion() {
        val state = _uiState.value as? DrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        if (state.session.isComplete) {
            SessionStore.save(state.session)
            _uiState.value = DrawQuizUiState.Complete(state.session.id)
        } else {
            _uiState.value = state.copy(
                currentFingering = emptyFingering(inst.stringCount),
                feedback = null,
                displayedQuestion = state.session.currentQuestion,
                displayedQuestionIndex = state.displayedQuestionIndex + 1
            )
        }
    }

    private fun emptyFingering(stringCount: Int) = Fingering(
        positions = (0 until stringCount).map { StringPosition(it, 0) }
    )
}
