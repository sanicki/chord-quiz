package com.chordquiz.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.repository.GroupsRepository
import com.chordquiz.app.data.repository.InstrumentRepository
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
    val activeTypeFilter: ChordType? = null,
    val activeGroupFilter: GroupEntity? = null,
    val customGroups: List<GroupEntity> = emptyList(),
    val isLoading: Boolean = true,
    val editingGroup: GroupEntity? = null,
    val staleChordWarning: Boolean = false,
    val deleteConfirmGroup: GroupEntity? = null,
    val showDiscardEditDialog: Boolean = false,
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

    private val typeFilter = MutableStateFlow<ChordType?>(null)
    private val groupFilter = MutableStateFlow<GroupEntity?>(null)

    // Held across the async validation → save flow
    private var pendingFilterChange: (() -> Unit)? = null
    private var pendingSaveName: String? = null
    private var pendingSaveInstrumentId: String? = null
    private var pendingSaveChordIds: List<String>? = null

    fun loadInstrument(instrumentId: String) {
        viewModelScope.launch {
            val instrument = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            combine(
                getChordsForInstrument(instrumentId),
                typeFilter,
                groupFilter,
                groupsRepository.getGroupsFlow(instrumentId)
            ) { chords, typeF, groupF, groups ->
                val existingIds = chords.map { it.id }.toSet()
                var filtered = chords
                var stale = false
                when {
                    groupF != null -> {
                        val groupIds = groupF.chordIdsList()
                        stale = groupIds.any { it !in existingIds }
                        filtered = chords.filter { it.id in groupIds }
                    }
                    typeF != null -> {
                        filtered = chords.filter { it.chordType == typeF }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    instrument = instrument,
                    allChords = chords,
                    filteredChords = filtered,
                    activeTypeFilter = typeF,
                    activeGroupFilter = groupF,
                    customGroups = groups.sortedByDescending { it.createdAt },
                    staleChordWarning = stale,
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

    fun setTypeFilter(type: ChordType?) {
        if (_uiState.value.editingGroup != null) {
            pendingFilterChange = {
                groupFilter.value = null
                typeFilter.value = type
                _uiState.value = _uiState.value.copy(
                    editingGroup = null,
                    showDiscardEditDialog = false,
                    selectedChordIds = emptySet()
                )
            }
            _uiState.value = _uiState.value.copy(showDiscardEditDialog = true)
            return
        }
        groupFilter.value = null
        typeFilter.value = type
    }

    fun setGroupFilter(group: GroupEntity?) {
        val editing = _uiState.value.editingGroup
        if (editing != null && group?.id != editing.id) {
            pendingFilterChange = {
                typeFilter.value = null
                groupFilter.value = group
                _uiState.value = _uiState.value.copy(
                    editingGroup = null,
                    showDiscardEditDialog = false,
                    selectedChordIds = emptySet()
                )
            }
            _uiState.value = _uiState.value.copy(showDiscardEditDialog = true)
            return
        }
        typeFilter.value = null
        groupFilter.value = group
    }

    fun startEdit(group: GroupEntity) {
        val existingIds = _uiState.value.allChords.map { it.id }.toSet()
        val selectedIds = group.chordIdsList().filter { it in existingIds }.toSet()
        typeFilter.value = null
        groupFilter.value = group
        _uiState.value = _uiState.value.copy(
            editingGroup = group,
            selectedChordIds = selectedIds
        )
    }

    fun requestDeleteGroup(group: GroupEntity) {
        _uiState.value = _uiState.value.copy(deleteConfirmGroup = group)
    }

    fun confirmDeleteGroup() {
        val group = _uiState.value.deleteConfirmGroup ?: return
        viewModelScope.launch {
            val wasActive = _uiState.value.activeGroupFilter?.id == group.id
            groupsRepository.deleteGroup(group.id)
            if (wasActive) {
                groupFilter.value = null
                _uiState.value = _uiState.value.copy(
                    deleteConfirmGroup = null,
                    editingGroup = null,
                    selectedChordIds = emptySet()
                )
            } else {
                _uiState.value = _uiState.value.copy(deleteConfirmGroup = null)
            }
        }
    }

    fun cancelDeleteGroup() {
        _uiState.value = _uiState.value.copy(deleteConfirmGroup = null)
    }

    fun confirmDiscardEdit() {
        pendingFilterChange?.invoke()
        pendingFilterChange = null
    }

    fun cancelDiscardEdit() {
        pendingFilterChange = null
        _uiState.value = _uiState.value.copy(showDiscardEditDialog = false)
    }

    fun requestSaveGroup(name: String, instrumentId: String, chordIds: List<String>) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || chordIds.size < 2) return

        val isPreset = ChordType.entries.any { it.displayName.equals(trimmed, ignoreCase = true) }
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
        val editingGroup = _uiState.value.editingGroup

        viewModelScope.launch {
            val existing = groupsRepository.findGroupByName(instrumentId, prefixedName)
            when {
                existing == null ->
                    doSaveGroup(trimmed, instrumentId, chordIds)
                editingGroup != null && existing.id == editingGroup.id ->
                    doSaveGroup(trimmed, instrumentId, chordIds)
                else ->
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
        val editingGroup = _uiState.value.editingGroup

        // Delete the group being edited (handles rename case)
        editingGroup?.let { groupsRepository.deleteGroup(it.id) }

        // Delete any other existing group with the same name (overwrite case)
        val existingWithName = groupsRepository.findGroupByName(instrumentId, prefixedName)
        if (existingWithName != null && existingWithName.id != editingGroup?.id) {
            groupsRepository.deleteGroup(existingWithName.id)
        }

        val newId = groupsRepository.insertGroup(
            GroupEntity(instrumentId = instrumentId, name = prefixedName, chordIds = chordIdsString)
        )

        // In edit mode, keep the active filter pointing at the new group
        if (editingGroup != null) {
            groupFilter.value = GroupEntity(
                id = newId,
                instrumentId = instrumentId,
                name = prefixedName,
                chordIds = chordIdsString
            )
        }

        _uiState.value = _uiState.value.copy(
            editingGroup = null,
            saveNameError = null
        )
        pendingSaveName = null
        pendingSaveInstrumentId = null
        pendingSaveChordIds = null
        _saveComplete.tryEmit(Unit)
    }
}
