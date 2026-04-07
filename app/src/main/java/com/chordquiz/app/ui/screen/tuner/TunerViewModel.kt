package com.chordquiz.app.ui.screen.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.AudioRecorderManager
import com.chordquiz.app.audio.NotePlayer
import com.chordquiz.app.audio.PitchDetector
import com.chordquiz.app.audio.StringTuningState
import com.chordquiz.app.audio.TunerPitchDetector
import com.chordquiz.app.audio.TunerStringMatcher
import com.chordquiz.app.audio.TuningZone
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.preferences.UserPreferencesRepository
import com.chordquiz.app.data.repository.InstrumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class TunerUiState(
    val instrument: Instrument = Instrument.GUITAR,
    val stringStates: List<StringTuningState> = emptyList(),
    val guidanceText: String? = null,
    val successMessage: String? = null,
    val amplitude: Float = 0f,
    val isListening: Boolean = false
)

@HiltViewModel
class TunerViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val audioRecorder: AudioRecorderManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var successDismissJob: Job? = null
    private var autoContinueDelaySeconds: Int = 2

    /** Sliding window for median-filter smoothing of detected pitch (Hz). */
    private val pitchWindow = ArrayDeque<Float>(MEDIAN_WINDOW)

    companion object {
        private const val MEDIAN_WINDOW = 5
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.autoContinueDelaySeconds.collect {
                autoContinueDelaySeconds = it
            }
        }
    }

    fun initialize(instrumentId: String) {
        viewModelScope.launch {
            val inst = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            _uiState.value = TunerUiState(
                instrument = inst,
                stringStates = TunerStringMatcher.match(null, inst),
                isListening = true
            )
            startListening()
        }
    }

    private fun startListening() {
        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            audioRecorder.audioBufferFlow().collect { buffer ->
                val state = _uiState.value
                if (!state.isListening) return@collect

                val amplitude = PitchDetector.computeAmplitude(buffer)

                // While showing a success message, only update the amplitude so the
                // waveform stays live without re-triggering detection.
                if (state.successMessage != null) {
                    _uiState.value = state.copy(amplitude = amplitude)
                    return@collect
                }

                val rawHz = TunerPitchDetector.detectPitch(buffer)
                val detectedHz = smoothPitch(rawHz)

                val stringStates = TunerStringMatcher.match(detectedHz, state.instrument)

                // Find the single active string (non-ambiguous, non-neutral)
                val activeString = stringStates.firstOrNull {
                    !it.isAmbiguous && it.centsDeviation != null
                }

                val guidanceText = when (activeString?.tuningZone) {
                    TuningZone.FLAT_RED, TuningZone.FLAT_YELLOW -> "Tune Up"
                    TuningZone.SHARP_RED, TuningZone.SHARP_YELLOW -> "Tune Down"
                    else -> null
                }

                _uiState.value = state.copy(
                    stringStates = stringStates,
                    guidanceText = guidanceText,
                    amplitude = amplitude
                )

                if (activeString?.tuningZone == TuningZone.IN_TUNE) {
                    onStringInTune(activeString, state.instrument)
                }
            }
        }
    }

    /**
     * Adds [rawHz] to the sliding window and returns the median, giving smooth
     * pitch readings even when the FFT produces occasional outliers.
     * Returns null when no pitch is detected (clears the window).
     */
    private fun smoothPitch(rawHz: Float?): Float? {
        if (rawHz == null || rawHz <= 0f) {
            pitchWindow.clear()
            return null
        }
        if (pitchWindow.size >= MEDIAN_WINDOW) pitchWindow.removeFirst()
        pitchWindow.addLast(rawHz)
        val sorted = pitchWindow.sorted()
        return sorted[sorted.size / 2]
    }

    private fun onStringInTune(stringState: StringTuningState, instrument: Instrument) {
        val state = _uiState.value
        // Guard: don't re-trigger if a success message is already showing
        if (state.successMessage != null) return

        val message = "${stringState.openNote.displayName} string in Tune!"
        // Keep isListening = true so the waveform stays live during the success overlay.
        _uiState.value = state.copy(
            successMessage = message,
            guidanceText = null
        )

        val midi = stringState.openNote.semitone + 12 * (stringState.octave + 1)
        viewModelScope.launch {
            NotePlayer.playNote(midi, instrument.id)
        }

        // Duration = 2× the user's auto-continue delay, minimum 2 000 ms.
        val dismissDelay = max(autoContinueDelaySeconds * 2 * 1000L, 2000L)
        successDismissJob?.cancel()
        successDismissJob = viewModelScope.launch {
            delay(dismissDelay)
            resetToIdle()
        }
    }

    private fun resetToIdle() {
        val inst = _uiState.value.instrument
        pitchWindow.clear()
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            guidanceText = null,
            amplitude = 0f,
            stringStates = TunerStringMatcher.match(null, inst),
            isListening = true
        )
        startListening()
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
        successDismissJob?.cancel()
        audioRecorder.stop()
    }
}
