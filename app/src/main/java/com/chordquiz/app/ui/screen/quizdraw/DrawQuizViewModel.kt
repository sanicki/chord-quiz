package com.chordquiz.app.ui.screen.quizdraw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.chordquiz.app.haptic.HapticManager
import com.chordquiz.app.domain.EvaluateDrawAnswerUseCase
import com.chordquiz.app.ui.shared.SessionStore
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
        val incorrectFrettedStrings: Set<Int> = emptySet(),
        val incorrectMutedStrings: Set<Int> = emptySet(),
        val missedMuteStrings: Set<Int> = emptySet(),
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
    private val evaluateAnswer: EvaluateDrawAnswerUseCase,
    private val hapticManager: HapticManager
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
        if (state.feedback != null) return  // locked during countdown
        _uiState.value = state.copy(
            currentFingering = fingering,
            feedback = null,
            incorrectFrettedStrings = emptySet(),
            incorrectMutedStrings = emptySet(),
            missedMuteStrings = emptySet()
        )
    }

    fun onNoteSelected(stringIndex: Int, fret: Int) {
        val state = _uiState.value as? DrawQuizUiState.Active ?: return
        if (state.feedback != null) return  // locked during countdown
        val inst = instrument ?: return
        if (fret < 0) return
        val openNote = inst.openStringNotes.getOrNull(stringIndex) ?: return
        val openOctave = inst.openStringOctaves.getOrNull(stringIndex) ?: return
        val midi = openNote.semitone + 12 * (openOctave + 1) + fret
        viewModelScope.launch {
            NotePlayer.playNote(midi, inst.id)
        }
    }

    fun submitAnswer() {
        val state = _uiState.value as? DrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        val question = state.displayedQuestion ?: state.session.currentQuestion ?: return

        val referenceFingering = question.chordDefinition.fingerings.getOrNull(question.targetFingeringIndex)
        val isCorrect = evaluateAnswer(inst, state.currentFingering, question.chordDefinition, referenceFingering)

        // Compute per-string feedback for incorrect answers
        val incorrectFrettedStrings = mutableSetOf<Int>()
        val incorrectMutedStrings = mutableSetOf<Int>()
        val missedMuteStrings = mutableSetOf<Int>()
        if (!isCorrect && referenceFingering != null) {
            val refByString = referenceFingering.positions.associateBy { it.stringIndex }
            val userByString = state.currentFingering.positions.associateBy { it.stringIndex }
            for (stringIndex in 0 until inst.stringCount) {
                val refFret = refByString[stringIndex]?.fret ?: 0
                val userFret = userByString[stringIndex]?.fret ?: 0
                when {
                    refFret == -1 && userFret != -1 -> missedMuteStrings.add(stringIndex)
                    refFret != -1 && userFret == -1 -> incorrectMutedStrings.add(stringIndex)
                    refFret >= 0 && userFret != refFret -> incorrectFrettedStrings.add(stringIndex)
                }
            }
        }

        val answer = QuizAnswer(
            question = question,
            isCorrect = isCorrect,
            userFingering = state.currentFingering
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = if (isCorrect) AnswerFeedback.CORRECT else AnswerFeedback.INCORRECT,
            incorrectFrettedStrings = incorrectFrettedStrings,
            incorrectMutedStrings = incorrectMutedStrings,
            missedMuteStrings = missedMuteStrings
        )

        if (!isCorrect) {
            viewModelScope.launch {
                hapticManager.vibrateWrongAnswer()
            }
        }

        if (isCorrect && referenceFingering != null) {
            val midis = referenceFingering.positions
                .filter { it.fret >= 0 }
                .mapNotNull { pos ->
                    val openNote = inst.openStringNotes.getOrNull(pos.stringIndex) ?: return@mapNotNull null
                    val openOctave = inst.openStringOctaves.getOrNull(pos.stringIndex) ?: return@mapNotNull null
                    openNote.semitone + 12 * (openOctave + 1) + pos.fret
                }
            viewModelScope.launch {
                NotePlayer.playChord(midis, inst.id)
            }
        }
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
                incorrectFrettedStrings = emptySet(),
                incorrectMutedStrings = emptySet(),
                missedMuteStrings = emptySet(),
                displayedQuestion = state.session.currentQuestion,
                displayedQuestionIndex = state.displayedQuestionIndex + 1
            )
        }
    }

    private fun emptyFingering(stringCount: Int) = Fingering(
        positions = (0 until stringCount).map { StringPosition(it, 0) }
    )
}
