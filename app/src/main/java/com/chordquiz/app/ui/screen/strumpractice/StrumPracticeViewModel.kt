package com.chordquiz.app.ui.screen.strumpractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.StrumMetronomeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StrumSlot { MUTE, DOWN, UP }

enum class StrumNote(val label: String, val slotCount: Int, val subdivisionsPerBeat: Double) {
    WHOLE("Whole", 4, 0.25),
    HALF("Half", 4, 0.5),
    QUARTER("Quarter", 4, 1.0),
    SIXTEENTH("16th", 16, 4.0)
}

data class StrumPracticeUiState(
    val isPlaying: Boolean = false,
    val bpm: Int = 90,
    val speedPercent: Int = 100,
    val noteType: StrumNote = StrumNote.QUARTER,
    val slots: List<StrumSlot> = List(4) { StrumSlot.MUTE },
    val activeSlotIndex: Int = -1
) {
    val effectiveBpm: Int get() = (bpm * speedPercent / 100.0).toInt()
}

@HiltViewModel
class StrumPracticeViewModel @Inject constructor(
    private val metronome: StrumMetronomeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrumPracticeUiState())
    val uiState: StateFlow<StrumPracticeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            metronome.tickFlow.collect { slotIndex ->
                _uiState.update { it.copy(activeSlotIndex = slotIndex) }
            }
        }
    }

    fun togglePlay() {
        val playing = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = playing, activeSlotIndex = -1) }
        if (playing) metronome.start() else metronome.stop()
    }

    fun adjustBpm(delta: Int) {
        val newBpm = (_uiState.value.bpm + delta).coerceIn(40, 240)
        _uiState.update { it.copy(bpm = newBpm) }
        metronome.updateBpm(newBpm)
    }

    fun adjustSpeedPercent(delta: Int) {
        val newPct = (_uiState.value.speedPercent + delta).coerceIn(1, 100)
        _uiState.update { it.copy(speedPercent = newPct) }
        metronome.updateSpeedPercent(newPct)
    }

    fun onNoteTypeChanged(noteType: StrumNote) {
        _uiState.update { state ->
            state.copy(
                noteType = noteType,
                slots = List(noteType.slotCount) { StrumSlot.MUTE },
                activeSlotIndex = -1
            )
        }
        metronome.updateNoteType(noteType.slotCount, noteType.subdivisionsPerBeat)
    }

    fun onSlotTapped(index: Int) {
        _uiState.update { state ->
            val next = when (state.slots[index]) {
                StrumSlot.MUTE -> StrumSlot.DOWN
                StrumSlot.DOWN -> StrumSlot.UP
                StrumSlot.UP -> StrumSlot.MUTE
            }
            val newSlots = state.slots.toMutableList().also { it[index] = next }
            state.copy(slots = newSlots)
        }
    }

    override fun onCleared() {
        super.onCleared()
        metronome.stop()
    }
}
