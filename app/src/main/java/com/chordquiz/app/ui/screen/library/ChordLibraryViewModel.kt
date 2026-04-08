package com.chordquiz.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.repository.GroupsRepository
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.domain.ChordDifficultyCalculator
import com.chordquiz.app.domain.GetChordsForInstrumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChordLibraryUiState(
    val instrument: Instrument? = null,
    val allChords: List<ChordDefinition> = emptyList(),
    val filteredChords: List<ChordDefinition> = emptyList(),
    val selectedChordIds: Set<String> = emptySet(),
    val activeGroupFilter: GroupEntity? = null,
    val difficultyGroups: List<GroupEntity> = emptyList(),
    val customGroups: List<GroupEntity> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmGroup: GroupEntity? = null,
    val saveNameError: String? = null,
    val showReplaceGroupDialog: Boolean = false,
    val replaceGroupDisplayName: String? = null
)

@HiltViewModel
class ChordLibraryViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val getChordsForInstrument: GetChordsForInstrumentUseCase,
    private val groupsRepository: GroupsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordLibraryUiState())
    val uiState: StateFlow<ChordLibraryUiState> = _uiState.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    private val groupFilter = MutableStateFlow<GroupEntity?>(null)

    // Held across the async validation → save flow
    private var pendingSaveName: String? = null
    private var pendingSaveInstrumentId: String? = null
    private var pendingSaveChordIds: List<String>? = null

    fun loadInstrument(instrumentId: String) {
        viewModelScope.launch {
            val instrument = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            combine(
                getChordsForInstrument(instrumentId),
                groupFilter,
                groupsRepository.getGroupsFlow(instrumentId)
            ) { chords, groupF, customGroups ->
                val difficultyGroups = groupsRepository.computeDifficultyGroups(instrumentId, chords)
                val filtered = if (groupF != null) {
                    val groupIds = groupF.chordIdsList()
                    chords.filter { it.id in groupIds }
                } else {
                    chords
                }
                _uiState.value = _uiState.value.copy(
                    instrument = instrument,
                    allChords = chords,
                    filteredChords = filtered,
                    activeGroupFilter = groupF,
                    difficultyGroups = difficultyGroups,
                    customGroups = customGroups.sortedByDescending { it.createdAt },
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun toggleChordSelection(chordId: String) {
        val current = _uiState.value.selectedChordIds.toMutableSet()
        if (current.contains(chordId)) current.remove(chordId) else current.add(chordId)
        _uiState.value = _uiState.value.copy(selectedChordIds = current)
    }

    fun setGroupFilter(group: GroupEntity?) {
        val wasOnGroup = groupFilter.value != null
        groupFilter.value = group
        if (group != null) {
            val existingIds = _uiState.value.allChords.map { it.id }.toSet()
            val selected = group.chordIdsList().filter { it in existingIds }.toSet()
            _uiState.value = _uiState.value.copy(selectedChordIds = selected)
        } else if (wasOnGroup) {
            _uiState.value = _uiState.value.copy(selectedChordIds = emptySet())
        }
    }

    fun requestDeleteGroup(group: GroupEntity) {
        _uiState.value = _uiState.value.copy(deleteConfirmGroup = group)
    }

    fun confirmDeleteGroup() {
        val group = _uiState.value.deleteConfirmGroup ?: return
        viewModelScope.launch {
            groupsRepository.deleteGroup(group.id)
            groupFilter.value = null
            _uiState.value = _uiState.value.copy(
                deleteConfirmGroup = null,
                selectedChordIds = emptySet()
            )
        }
    }

    fun cancelDeleteGroup() {
        _uiState.value = _uiState.value.copy(deleteConfirmGroup = null)
    }

    fun requestSaveGroup(name: String, instrumentId: String, chordIds: List<String>) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || chordIds.size < 2) return

        val isPreset = trimmed.equals("Easy", ignoreCase = true) ||
                trimmed.equals("Moderate", ignoreCase = true) ||
                trimmed.equals("Difficult", ignoreCase = true)
        if (isPreset) {
            _uiState.value = _uiState.value.copy(
                saveNameError = "This name is already used by a built-in group."
            )
            return
        }

        pendingSaveName = trimmed
        pendingSaveInstrumentId = instrumentId
        pendingSaveChordIds = chordIds

        val prefixedName = "Custom:$trimmed"

        viewModelScope.launch {
            val existing = groupsRepository.findGroupByName(instrumentId, prefixedName)
            if (existing == null) {
                doSaveGroup(trimmed, instrumentId, chordIds)
            } else {
                _uiState.value = _uiState.value.copy(
                    showReplaceGroupDialog = true,
                    replaceGroupDisplayName = trimmed
                )
            }
        }
    }

    fun confirmReplaceGroup() {
        val name = pendingSaveName ?: return
        val instrumentId = pendingSaveInstrumentId ?: return
        val chordIds = pendingSaveChordIds ?: return
        _uiState.value = _uiState.value.copy(showReplaceGroupDialog = false)
        viewModelScope.launch { doSaveGroup(name, instrumentId, chordIds) }
    }

    fun cancelReplaceGroup() {
        _uiState.value = _uiState.value.copy(
            showReplaceGroupDialog = false,
            replaceGroupDisplayName = null
        )
    }

    fun clearSaveNameError() {
        if (_uiState.value.saveNameError != null) {
            _uiState.value = _uiState.value.copy(saveNameError = null)
        }
    }

    fun selectAll() {
        val ids = _uiState.value.filteredChords.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedChordIds = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedChordIds = emptySet())
    }

    private suspend fun doSaveGroup(name: String, instrumentId: String, chordIds: List<String>) {
        val prefixedName = "Custom:$name"
        val chordIdsString = chordIds.joinToString(",")

        val existingWithName = groupsRepository.findGroupByName(instrumentId, prefixedName)
        existingWithName?.let { groupsRepository.deleteGroup(it.id) }

        groupsRepository.insertGroup(
            GroupEntity(instrumentId = instrumentId, name = prefixedName, chordIds = chordIdsString)
        )
        pendingSaveName = null
        pendingSaveInstrumentId = null
        pendingSaveChordIds = null
        _saveComplete.tryEmit(Unit)
    }
}
