package com.chordquiz.app.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.db.entity.GroupEntity
import com.chordquiz.app.data.repository.GroupsRepository
import com.chordquiz.app.data.repository.InstrumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<GroupEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupsRepository: GroupsRepository,
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    fun loadGroups(instrumentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                groupsRepository.getGroupsFlow(instrumentId).collect { groups ->
                    _uiState.value = _uiState.value.copy(
                        groups = groups,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load groups"
                )
            }
        }
    }

    fun setName(value: String) {
        _groupName.value = value
    }

    fun saveSelectedAsGroup(instrumentId: String, selectedChordIds: List<String>) {
        if (_groupName.value.isBlank()) return

        val groupName = "Custom:${_groupName.value.trim()}"
        val chordIdsString = selectedChordIds.joinToString(",")
        viewModelScope.launch {
            try {
                // Check if group with same name exists
                val existingGroup = groupsRepository.findGroupByName(instrumentId, groupName)
                if (existingGroup != null) {
                    // Delete old group first
                    groupsRepository.deleteGroup(existingGroup.id)
                }

                // Create new group
                val newGroup = GroupEntity(
                    id = 0L, // Auto-generated
                    instrumentId = instrumentId,
                    name = groupName,
                    chordIds = chordIdsString
                )
                groupsRepository.insertGroup(newGroup)

                // Clear name input
                _groupName.value = ""
            } catch (e: Exception) {
                // Error handled via state
            }
        }
    }

    fun clearGroupName() {
        _groupName.value = ""
    }
}