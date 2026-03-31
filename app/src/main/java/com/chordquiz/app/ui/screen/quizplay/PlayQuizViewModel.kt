package com.chordquiz.app.ui.screen.quizplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.AudioRecorderManager
import com.chordquiz.app.audio.ChordRecognizer
import com.chordquiz.app.audio.NotePlayer
import com.chordquiz.app.audio.PitchDetector
import com.chordquiz.app.audio.RecognitionResult
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.QuizAnswer
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.data.repository.ChordRepository
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.BuildQuizSessionUseCase
import com.chordquiz.app.domain.EvaluateAudioAnswerUseCase
import com.chordquiz.app.ui.screen.results.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayQuizUiState {
    object Loading : PlayQuizUiState()
    data class Active(
        val session: QuizSession,
        val amplitude: Float = 0f,
        val detectedNotes: List<Note> = emptyList(),
        val recognition: RecognitionResult? = null,
        val feedback: PlayFeedback? = null,
        val isListening: Boolean = true
    ) : PlayQuizUiState()
    data class Complete(val sessionId: String) : PlayQuizUiState()
}

enum class PlayFeedback { CORRECT, INCORRECT }

@HiltViewModel
class PlayQuizViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val chordRepo: ChordRepository,
    private val buildSession: BuildQuizSessionUseCase,
    private val evaluateAudio: EvaluateAudioAnswerUseCase,
    private val audioRecorder: AudioRecorderManager,
    private val chordRecognizer: ChordRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayQuizUiState>(PlayQuizUiState.Loading)
    val uiState: StateFlow<PlayQuizUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var instrument: Instrument? = null

    companion object {
        private const val SILENCE_THRESHOLD = 0.02f
    }

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

            chordRecognizer.setCandidates(selected)
            val session = buildSession(inst, QuizMode.PLAY, selected, questionCount, repeatMissed)
            _uiState.value = PlayQuizUiState.Active(session = session)
            startListening()
        }
    }

    private fun startListening() {
        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            audioRecorder.audioBufferFlow().collect { buffer ->
                val state = _uiState.value as? PlayQuizUiState.Active ?: return@collect
                if (!state.isListening) return@collect

                val amplitude = PitchDetector.computeAmplitude(buffer)
                // Gate pitch detection: ignore silent/near-silent input
                val pitches = if (amplitude >= SILENCE_THRESHOLD) {
                    PitchDetector.detectPitches(buffer)
                } else {
                    emptyList()
                }
                val recognition = if (pitches.isNotEmpty()) chordRecognizer.recognize(pitches) else null
                val notes = recognition?.detectedNotes?.toList() ?: emptyList()

                val newState = state.copy(
                    amplitude = amplitude,
                    detectedNotes = notes,
                    recognition = recognition
                )

                // If high-confidence match to the current question chord
                val question = state.session.currentQuestion
                if (recognition != null && question != null &&
                    recognition.chord.id == question.chordDefinition.id &&
                    recognition.confidence >= 0.6f) {
                    onChordDetected(newState, isCorrect = true)
                } else {
                    _uiState.value = newState
                }
            }
        }
    }

    private fun onChordDetected(state: PlayQuizUiState.Active, isCorrect: Boolean) {
        val question = state.session.currentQuestion ?: return
        val answer = QuizAnswer(
            question = question,
            isCorrect = isCorrect,
            detectedNotes = state.detectedNotes
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = if (isCorrect) PlayFeedback.CORRECT else PlayFeedback.INCORRECT,
            isListening = false
        )

        // Play the chord back in the instrument's voice when correct
        if (isCorrect) {
            val inst = instrument
            val fingering = question.chordDefinition.fingerings.firstOrNull()
            if (inst != null && fingering != null) {
                val midis = fingering.positions
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

        // Auto-advance after 2 seconds
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(2000)
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? PlayQuizUiState.Active ?: return
        if (state.session.isComplete) {
            SessionStore.save(state.session)
            _uiState.value = PlayQuizUiState.Complete(state.session.id)
            listeningJob?.cancel()
        } else {
            _uiState.value = state.copy(
                amplitude = 0f,
                detectedNotes = emptyList(),
                recognition = null,
                feedback = null,
                isListening = true
            )
            startListening()
        }
    }

    fun skipQuestion() {
        val state = _uiState.value as? PlayQuizUiState.Active ?: return
        val question = state.session.currentQuestion ?: return
        val answer = QuizAnswer(
            question = question,
            isCorrect = false,
            detectedNotes = emptyList()
        )
        val newSession = state.session.copy(answers = state.session.answers + answer)
        _uiState.value = state.copy(
            session = newSession,
            feedback = PlayFeedback.INCORRECT,
            isListening = false
        )
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(1500)
            nextQuestion()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
        autoAdvanceJob?.cancel()
        audioRecorder.stop()
    }
}
