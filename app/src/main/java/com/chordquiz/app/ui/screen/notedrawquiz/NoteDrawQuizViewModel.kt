package com.chordquiz.app.ui.screen.notedrawquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.NotePlayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

sealed class NoteDrawQuizUiState {
    object Loading : NoteDrawQuizUiState()
    data class Active(
        val session: QuizSession,
        val currentFingering: Fingering,
        val feedback: NoteDrawFeedback? = null,
        /** Yellow hint dots shown after a wrong tap in single-note modes: (stringIndex, fret) */
        val hintPositions: Set<Pair<Int, Int>> = emptySet(),
        /** Correctly placed positions accumulated during FIND_ALL modes */
        val correctlyPlacedPositions: Set<Pair<Int, Int>> = emptySet(),
        /** Increments on wrong tap in FIND_ALL modes to reset the diagram to correct-only state */
        val wrongTapResetKey: Int = 0,
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
    private var startTime: Long = 0L
    private var autoAdvanceJob: Job? = null

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
            startTime = System.currentTimeMillis()
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
        if (state.feedback != null) return  // locked during countdown
        val inst = instrument ?: return
        val question = state.displayedQuestion ?: state.session.currentQuestion ?: return
        val noteQuestion = question as? QuizQuestion.NoteQuestion ?: return

        // Find a newly placed position (fret changed from -1 to anything >= 0)
        val oldByString = state.currentFingering.positions.associateBy { it.stringIndex }
        val newlyPlaced = fingering.positions.firstOrNull { newPos ->
            val oldFret = oldByString[newPos.stringIndex]?.fret ?: -1
            newPos.fret >= 0 && newPos.fret != oldFret
        }

        if (newlyPlaced == null) {
            // Removal or no meaningful change — update silently
            _uiState.value = state.copy(currentFingering = fingering)
            return
        }

        when (noteQuestion.noteMode) {
            NoteMode.FIND_NOTE, NoteMode.FIND_NOTE_CORRECT_OCTAVE -> {
                // Single-position mode: each placement is an immediate submission
                val isCorrect = isPositionCorrect(inst, newlyPlaced, noteQuestion)
                val answer = QuizAnswer(
                    question = noteQuestion,
                    isCorrect = isCorrect,
                    userFingering = fingering
                )
                val newSession = state.session.copy(answers = state.session.answers + answer)
                val hints = if (!isCorrect) {
                    evaluateNoteDrawAnswer
                        .computeAllPositionsForQuestion(inst, noteQuestion)
                        .map { Pair(it.stringIndex, it.fret) }
                        .toSet()
                } else emptySet()

                _uiState.value = state.copy(
                    session = newSession,
                    currentFingering = fingering,
                    feedback = if (isCorrect) NoteDrawFeedback.CORRECT else NoteDrawFeedback.INCORRECT,
                    hintPositions = hints
                )

                if (!isCorrect) {
                    viewModelScope.launch { hapticManager.vibrateWrongAnswer() }
                }
            }

            NoteMode.FIND_ALL_NOTES, NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE -> {
                // All-positions mode: validate each tap individually
                val isCorrect = isPositionCorrect(inst, newlyPlaced, noteQuestion)

                if (!isCorrect) {
                    // Wrong note tapped: show feedback, reset diagram to correct-only state
                    val correctFingering = buildPartialFingering(inst.stringCount, state.correctlyPlacedPositions)
                    _uiState.value = state.copy(
                        currentFingering = correctFingering,
                        feedback = NoteDrawFeedback.INCORRECT,
                        wrongTapResetKey = state.wrongTapResetKey + 1
                    )
                    viewModelScope.launch { hapticManager.vibrateWrongAnswer() }
                    // Clear feedback after a short delay without advancing
                    viewModelScope.launch {
                        delay(1000L)
                        val current = _uiState.value as? NoteDrawQuizUiState.Active ?: return@launch
                        if (current.feedback == NoteDrawFeedback.INCORRECT) {
                            _uiState.value = current.copy(feedback = null)
                        }
                    }
                } else {
                    // Correct placement — add to correctly placed set
                    val newCorrect = state.correctlyPlacedPositions +
                        Pair(newlyPlaced.stringIndex, newlyPlaced.fret)
                    val allPositions = evaluateNoteDrawAnswer
                        .computeAllPositionsForQuestion(inst, noteQuestion)
                    val isComplete = allPositions.all { pos ->
                        Pair(pos.stringIndex, pos.fret) in newCorrect
                    }

                    if (isComplete) {
                        // All positions found — auto-submit as correct
                        val answer = QuizAnswer(
                            question = noteQuestion,
                            isCorrect = true,
                            userFingering = fingering
                        )
                        val newSession = state.session.copy(answers = state.session.answers + answer)
                        _uiState.value = state.copy(
                            session = newSession,
                            currentFingering = fingering,
                            feedback = NoteDrawFeedback.CORRECT,
                            correctlyPlacedPositions = newCorrect
                        )
                    } else {
                        // Not yet complete — update silently
                        _uiState.value = state.copy(
                            currentFingering = fingering,
                            correctlyPlacedPositions = newCorrect
                        )
                    }
                }
            }
        }
    }

    fun onNoteSelected(stringIndex: Int, fret: Int) {
        val inst = instrument ?: return
        if (fret < 0) return
        val openNote = inst.openStringNotes.getOrNull(stringIndex) ?: return
        val openOctave = inst.openStringOctaves.getOrNull(stringIndex) ?: return
        val midi = openNote.semitone + 12 * (openOctave + 1) + fret
        viewModelScope.launch {
            NotePlayer.playNote(midi, inst.id)
        }
    }

    fun skipQuestion() {
        val state = _uiState.value as? NoteDrawQuizUiState.Active ?: return
        val question = state.displayedQuestion ?: state.session.currentQuestion ?: return
        val noteQuestion = question as? QuizQuestion.NoteQuestion ?: return
        val answer = QuizAnswer(
            question = noteQuestion,
            isCorrect = false,
            userFingering = state.currentFingering
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = NoteDrawFeedback.INCORRECT
        )
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(1500)
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? NoteDrawQuizUiState.Active ?: return
        val inst = instrument ?: return
        if (state.session.isComplete) {
            val finalSession = state.session.copy(
                totalDurationMillis = System.currentTimeMillis() - startTime
            )
            SessionStore.save(finalSession)
            _uiState.value = NoteDrawQuizUiState.Complete(finalSession.id)
        } else {
            val nextQuestion = state.session.currentQuestion
            _uiState.value = state.copy(
                currentFingering = emptyFingering(inst.stringCount),
                feedback = null,
                hintPositions = emptySet(),
                correctlyPlacedPositions = emptySet(),
                wrongTapResetKey = 0,
                displayedQuestion = nextQuestion,
                displayedQuestionIndex = state.displayedQuestionIndex + 1
            )
        }
    }

    private fun isPositionCorrect(
        instrument: Instrument,
        pos: StringPosition,
        question: QuizQuestion.NoteQuestion
    ): Boolean {
        val openNote = instrument.openStringNotes.getOrNull(pos.stringIndex) ?: return false
        val openOctave = instrument.openStringOctaves.getOrNull(pos.stringIndex) ?: return false
        val totalSemitones = openNote.semitone + pos.fret
        val semitone = totalSemitones % 12
        val octave = openOctave + totalSemitones / 12
        return when (question.noteMode) {
            NoteMode.FIND_NOTE, NoteMode.FIND_ALL_NOTES ->
                semitone == question.note.semitone
            NoteMode.FIND_NOTE_CORRECT_OCTAVE, NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE ->
                semitone == question.note.semitone && octave == question.octave
        }
    }

    private fun buildPartialFingering(
        stringCount: Int,
        correct: Set<Pair<Int, Int>>
    ): Fingering {
        val positions = (0 until stringCount).map { s ->
            val correctFret = correct.firstOrNull { it.first == s }?.second
            StringPosition(s, correctFret ?: -1)
        }
        return Fingering(positions = positions)
    }

    private fun emptyFingering(stringCount: Int) = Fingering(
        positions = (0 until stringCount).map { StringPosition(it, -1) }
    )
}
