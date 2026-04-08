package com.chordquiz.app.ui.screen.noteplayquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.AudioRecorderManager
import com.chordquiz.app.audio.PitchDetector
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizAnswer
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.BuildNoteQuizSessionUseCase
import com.chordquiz.app.domain.EvaluateNoteAudioAnswerUseCase
import com.chordquiz.app.ui.shared.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log2
import kotlin.math.roundToInt

sealed class NotePlayQuizUiState {
    object Loading : NotePlayQuizUiState()
    data class Active(
        val session: QuizSession,
        val amplitude: Float = 0f,
        val detectedNote: Note? = null,
        val detectedOctave: Int? = null,
        val feedback: NotePlayFeedback? = null,
        val isListening: Boolean = true
    ) : NotePlayQuizUiState()
    data class Complete(val sessionId: String) : NotePlayQuizUiState()
}

enum class NotePlayFeedback { CORRECT, INCORRECT }

val NotePlayFeedback.isCorrect: Boolean get() = this == NotePlayFeedback.CORRECT

@HiltViewModel
class NotePlayQuizViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val buildNoteSession: BuildNoteQuizSessionUseCase,
    private val evaluateNoteAudio: EvaluateNoteAudioAnswerUseCase,
    private val audioRecorder: AudioRecorderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotePlayQuizUiState>(NotePlayQuizUiState.Loading)
    val uiState: StateFlow<NotePlayQuizUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var autoAdvanceJob: Job? = null

    companion object {
        private const val SILENCE_THRESHOLD = 0.02f
        private const val A4_HZ = 440.0
        private const val A4_MIDI = 69
    }

    fun initialize(
        instrumentId: String,
        noteMode: NoteMode,
        questionCount: Int,
        repeatMissed: Boolean
    ) {
        viewModelScope.launch {
            val inst = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            val session = buildNoteSession.buildPlaySession(inst, noteMode, questionCount, repeatMissed)
            _uiState.value = NotePlayQuizUiState.Active(session = session)
            startListening()
        }
    }

    private fun startListening() {
        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            audioRecorder.audioBufferFlow().collect { buffer ->
                val state = _uiState.value as? NotePlayQuizUiState.Active ?: return@collect
                if (!state.isListening) return@collect

                val amplitude = PitchDetector.computeAmplitude(buffer)
                if (amplitude < SILENCE_THRESHOLD) {
                    _uiState.value = state.copy(amplitude = amplitude, detectedNote = null, detectedOctave = null)
                    return@collect
                }

                val pitches = PitchDetector.detectPitches(buffer)
                if (pitches.isEmpty()) {
                    _uiState.value = state.copy(amplitude = amplitude)
                    return@collect
                }

                // Take the strongest (first) pitch
                val hz = pitches.first().frequencyHz
                val (note, octave) = hzToNoteAndOctave(hz)

                val question = state.session.currentQuestion
                val noteQuestion = question as? QuizQuestion.NoteQuestion

                if (noteQuestion != null && evaluateNoteAudio(note, octave, noteQuestion)) {
                    val answer = QuizAnswer(
                        question = noteQuestion,
                        isCorrect = true,
                        detectedNote = note
                    )
                    val newSession = state.session.copy(answers = state.session.answers + answer)
                    _uiState.value = state.copy(
                        session = newSession,
                        amplitude = amplitude,
                        detectedNote = note,
                        detectedOctave = octave,
                        feedback = NotePlayFeedback.CORRECT,
                        isListening = false
                    )
                    autoAdvanceJob?.cancel()
                    autoAdvanceJob = viewModelScope.launch {
                        delay(2000)
                        nextQuestion()
                    }
                } else {
                    _uiState.value = state.copy(
                        amplitude = amplitude,
                        detectedNote = note,
                        detectedOctave = octave
                    )
                }
            }
        }
    }

    fun skipQuestion() {
        val state = _uiState.value as? NotePlayQuizUiState.Active ?: return
        val question = state.session.currentQuestion as? QuizQuestion.NoteQuestion ?: return
        val answer = QuizAnswer(
            question = question,
            isCorrect = false
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = NotePlayFeedback.INCORRECT,
            isListening = false
        )
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(1500)
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? NotePlayQuizUiState.Active ?: return
        if (state.session.isComplete) {
            SessionStore.save(state.session)
            _uiState.value = NotePlayQuizUiState.Complete(state.session.id)
            listeningJob?.cancel()
        } else {
            _uiState.value = state.copy(
                amplitude = 0f,
                detectedNote = null,
                detectedOctave = null,
                feedback = null,
                isListening = true
            )
            startListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
        autoAdvanceJob?.cancel()
        audioRecorder.stop()
    }

    private fun hzToNoteAndOctave(hz: Double): Pair<Note, Int> {
        // Convert frequency to MIDI note number
        val midi = (12.0 * log2(hz / A4_HZ) + A4_MIDI).roundToInt()
        val semitone = ((midi % 12) + 12) % 12
        val octave = midi / 12 - 1
        return Pair(Note.fromSemitone(semitone), octave)
    }
}
