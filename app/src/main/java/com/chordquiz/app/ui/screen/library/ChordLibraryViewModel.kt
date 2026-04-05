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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val isLoading: Boolean = true
)

@HiltViewModel
class ChordLibraryViewModel @Inject constructor(
    private val instrumentRepo: InstrumentRepository,
    private val getChordsForInstrument: GetChordsForInstrumentUseCase,
    private val groupsRepository: GroupsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordLibraryUiState())
    val uiState: StateFlow<ChordLibraryUiState> = _uiState.asStateFlow()

    private val typeFilter = MutableStateFlow<ChordType?>(null)
    private val groupFilter = MutableStateFlow<GroupEntity?>(null)

    fun loadInstrument(instrumentId: String) {
        viewModelScope.launch {
            val instrument = instrumentRepo.getInstrumentById(instrumentId) ?: return@launch
            combine(
                getChordsForInstrument(instrumentId),
                typeFilter,
                groupFilter,
                groupsRepository.getGroupsFlow(instrumentId)
            ) { chords, typeF, groupF, groups ->
                val filtered = when {
                    groupF != null -> chords.filter { it.id in groupF.chordIdsList() }
                    typeF != null -> chords.filter { it.chordType == typeF }
                    else -> chords
                }
                _uiState.value = _uiState.value.copy(
                    instrument = instrument,
                    allChords = chords,
                    filteredChords = filtered,
                    activeTypeFilter = typeF,
                    activeGroupFilter = groupF,
                    customGroups = groups.sortedByDescending { it.createdAt },
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
        groupFilter.value = null
        typeFilter.value = type
    }

    fun setGroupFilter(group: GroupEntity?) {
        typeFilter.value = null
        groupFilter.value = group
    }

    fun selectAll() {
        val ids = _uiState.value.filteredChords.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedChordIds = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedChordIds = emptySet())
    }

    fun saveGroup(name: String, instrumentId: String, chordIds: List<String>) {
        if (name.isBlank() || chordIds.size < 2) return
        val prefixedName = "Custom:${name.trim()}"
        val chordIdsString = chordIds.joinToString(",")
        viewModelScope.launch {
            val existing = groupsRepository.findGroupByName(instrumentId, prefixedName)
            existing?.let { groupsRepository.deleteGroup(it.id) }
            groupsRepository.insertGroup(
                GroupEntity(instrumentId = instrumentId, name = prefixedName, chordIds = chordIdsString)
            )
        }
    }
}
