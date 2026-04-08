package com.chordquiz.app.ui.screen.strumpractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.audio.StrumMetronomeManager
import com.chordquiz.app.data.db.entity.SavedPatternEntity
import com.chordquiz.app.data.repository.SavedPatternsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StrumSlot { MUTE, DOWN, UP }

enum class StrumNote(val slotCount: Int, val subdivisionsPerBeat: Double, val slotLabels: List<String>) {
    WHOLE(1,   0.25, listOf("1")),
    HALF( 2,   0.5,  listOf("1", "2")),
    QUARTER(4, 1.0,  listOf("1", "2", "3", "4")),
    EIGHTH( 8, 2.0,  listOf("1", "+", "2", "+", "3", "+", "4", "+"))
}

private fun defaultSlotsFor(noteType: StrumNote): List<StrumSlot> = when (noteType) {
    StrumNote.EIGHTH -> List(8) { i -> if (i % 2 == 0) StrumSlot.DOWN else StrumSlot.MUTE }
    else -> List(noteType.slotCount) { StrumSlot.MUTE }
}

data class StrumPracticeUiState(
    val isPlaying: Boolean = false,
    val bpm: Int = 90,
    val speedPercent: Int = 100,
    val noteType: StrumNote = StrumNote.EIGHTH,
    val slots: List<StrumSlot> = defaultSlotsFor(StrumNote.EIGHTH),
    val activeSlotIndex: Int = -1,
    val savedPatterns: List<SavedPatternEntity> = emptyList(),
    val showSaveDialog: Boolean = false,
    val saveNameError: String? = null,
    val showReplaceDialog: Boolean = false,
    val replacePatternName: String? = null,
    val deleteConfirmPattern: SavedPatternEntity? = null
) {
    val effectiveBpm: Int get() = (bpm * speedPercent / 100.0).toInt()
}

@HiltViewModel
class StrumPracticeViewModel @Inject constructor(
    private val metronome: StrumMetronomeManager,
    private val patternsRepository: SavedPatternsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrumPracticeUiState())
    val uiState: StateFlow<StrumPracticeUiState> = _uiState.asStateFlow()

    // held across the async duplicate-check → replace flow
    private var pendingSaveName: String? = null

    init {
        viewModelScope.launch {
            metronome.tickFlow.collect { slotIndex ->
                _uiState.update { it.copy(activeSlotIndex = slotIndex) }
            }
        }
        viewModelScope.launch {
            patternsRepository.getPatternsFlow().collect { patterns ->
                _uiState.update { it.copy(savedPatterns = patterns) }
            }
        }

        // Initialise metronome with default note type
        metronome.updateNoteType(StrumNote.EIGHTH.slotCount, StrumNote.EIGHTH.subdivisionsPerBeat)
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
                slots = defaultSlotsFor(noteType),
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
                StrumSlot.UP   -> StrumSlot.MUTE
            }
            val newSlots = state.slots.toMutableList().also { it[index] = next }
            state.copy(slots = newSlots)
        }
    }

    // ── Save dialog ──────────────────────────────────────────────────────────

    fun showSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = true, saveNameError = null) }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false, saveNameError = null) }
    }

    fun clearSaveNameError() {
        _uiState.update { it.copy(saveNameError = null) }
    }

    fun requestSavePattern(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(saveNameError = "Name cannot be blank.") }
            return
        }
        pendingSaveName = trimmed
        viewModelScope.launch {
            val existing = patternsRepository.findByName("Custom:$trimmed")
            if (existing == null) {
                doSavePattern(trimmed)
            } else {
                _uiState.update { it.copy(showReplaceDialog = true, replacePatternName = trimmed) }
            }
        }
    }

    fun confirmReplacePattern() {
        val name = pendingSaveName ?: return
        _uiState.update { it.copy(showReplaceDialog = false) }
        viewModelScope.launch { doSavePattern(name) }
    }

    fun cancelReplacePattern() {
        _uiState.update { it.copy(showReplaceDialog = false, replacePatternName = null) }
    }

    private suspend fun doSavePattern(name: String) {
        val prefixed = "Custom:$name"
        val existing = patternsRepository.findByName(prefixed)
        existing?.let { patternsRepository.deletePattern(it.id) }
        val state = _uiState.value
        patternsRepository.insertPattern(
            SavedPatternEntity(
                name = prefixed,
                noteType = state.noteType.name,
                slots = state.slots.joinToString(",") { it.name },
                bpm = state.bpm
            )
        )
        pendingSaveName = null
        _uiState.update { it.copy(showSaveDialog = false, saveNameError = null) }
    }

    // ── Load / Delete ────────────────────────────────────────────────────────

    fun loadPattern(pattern: SavedPatternEntity) {
        val noteType = runCatching { StrumNote.valueOf(pattern.noteType) }.getOrNull() ?: return
        val slots = pattern.slotList().mapNotNull { runCatching { StrumSlot.valueOf(it) }.getOrNull() }
        if (slots.size != noteType.slotCount) return
        _uiState.update { it.copy(noteType = noteType, slots = slots, bpm = pattern.bpm, activeSlotIndex = -1) }
        metronome.updateNoteType(noteType.slotCount, noteType.subdivisionsPerBeat)
        metronome.updateBpm(pattern.bpm)
    }

    fun requestDeletePattern(pattern: SavedPatternEntity) {
        _uiState.update { it.copy(deleteConfirmPattern = pattern) }
    }

    fun confirmDeletePattern() {
        val pattern = _uiState.value.deleteConfirmPattern ?: return
        viewModelScope.launch {
            patternsRepository.deletePattern(pattern.id)
            _uiState.update { it.copy(deleteConfirmPattern = null) }
        }
    }

    fun cancelDeletePattern() {
        _uiState.update { it.copy(deleteConfirmPattern = null) }
    }

    override fun onCleared() {
        super.onCleared()
        metronome.stop()
    }
}
