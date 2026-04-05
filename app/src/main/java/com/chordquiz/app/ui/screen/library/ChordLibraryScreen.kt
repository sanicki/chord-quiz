package com.chordquiz.app.ui.screen.library

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.ui.components.chord.ChordDiagram
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChordLibraryScreen(
    instrumentId: String,
    onNavigateBack: () -> Unit,
    onStartPractice: (String, List<String>) -> Unit,
    viewModel: ChordLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    var dialogInitialName by remember { mutableStateOf("") }
    var groupName by remember(showSaveDialog) { mutableStateOf(dialogInitialName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(instrumentId) {
        viewModel.loadInstrument(instrumentId)
    }

    // Close the naming dialog when save completes
    LaunchedEffect(viewModel) {
        viewModel.saveComplete.collect {
            showSaveDialog = false
            dialogInitialName = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.instrument?.displayName ?: "Chords") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedChordIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.selectedChordIds.size >= 2) {
                        OutlinedButton(
                            onClick = {
                                dialogInitialName = uiState.editingGroup?.toName() ?: ""
                                showSaveDialog = true
                            }
                        ) {
                            Text(if (uiState.editingGroup != null) "Update" else "Save")
                        }
                    }
                    Button(
                        onClick = {
                            onStartPractice(instrumentId, uiState.selectedChordIds.toList())
                        }
                    ) {
                        Text("Start →")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Selection info + Select All / None
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.selectedChordIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row {
                        TextButton(onClick = { viewModel.selectAll() }) { Text("All") }
                        TextButton(onClick = { viewModel.clearSelection() }) { Text("None") }
                    }
                }

                // Filter chips: All → custom groups (newest first) → preset types
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.activeTypeFilter == null && uiState.activeGroupFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("All") }
                    )
                    uiState.customGroups.forEach { group ->
                        key(group.id) {
                            var showMenu by remember { mutableStateOf(false) }
                            val editingGroupId = uiState.editingGroup?.id
                            Box {
                                FilterChip(
                                    selected = uiState.activeGroupFilter?.id == group.id,
                                    onClick = { viewModel.setGroupFilter(group) },
                                    label = { Text(group.toName()) },
                                    modifier = Modifier.pointerInput(group.id, editingGroupId) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (editingGroupId == null || editingGroupId == group.id) {
                                                    showMenu = true
                                                }
                                            }
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.startEdit(group)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.requestDeleteGroup(group)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    ChordType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.activeTypeFilter == type,
                            onClick = { viewModel.setTypeFilter(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                // Stale chord warning
                if (uiState.staleChordWarning) {
                    Text(
                        text = "Some chords in this group are no longer available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Chord grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    gridItems(uiState.filteredChords) { chord ->
                        val isSelected = chord.id in uiState.selectedChordIds
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.toggleChordSelection(chord.id) }
                                .padding(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ChordDiagram(
                                    chord = chord,
                                    modifier = Modifier.size(width = 80.dp, height = 96.dp)
                                )
                                Text(
                                    text = chord.chordName,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            if (isSelected) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Naming dialog (Save / Update) ---
        if (showSaveDialog) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            AlertDialog(
                onDismissRequest = {
                    showSaveDialog = false
                    viewModel.clearSaveNameError()
                },
                title = {
                    Text(if (uiState.editingGroup != null) "Update Group" else "Save as New Group")
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = {
                                groupName = it
                                viewModel.clearSaveNameError()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            label = { Text("Group name") },
                            singleLine = true,
                            isError = uiState.saveNameError != null,
                            supportingText = if (uiState.saveNameError != null) {
                                { Text(uiState.saveNameError!!) }
                            } else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.requestSaveGroup(
                                    groupName, instrumentId, uiState.selectedChordIds.toList()
                                )
                            })
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.requestSaveGroup(
                                groupName, instrumentId, uiState.selectedChordIds.toList()
                            )
                        },
                        enabled = groupName.isNotBlank() && uiState.selectedChordIds.size >= 2
                    ) {
                        Text(if (uiState.editingGroup != null) "Update" else "Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        viewModel.clearSaveNameError()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Replace existing group dialog ---
        if (uiState.showReplaceGroupDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelReplaceGroup() },
                title = { Text("Replace Group?") },
                text = {
                    Text(
                        "A group named \"${uiState.replaceGroupDisplayName}\" already exists. Replace it?"
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.confirmReplaceGroup()
                        showSaveDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelReplaceGroup() }) {
                        Text("No")
                    }
                }
            )
        }

        // --- Delete confirmation dialog ---
        uiState.deleteConfirmGroup?.let { group ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteGroup() },
                title = { Text("Delete ${group.toName()}?") },
                confirmButton = {
                    Button(onClick = { viewModel.confirmDeleteGroup() }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteGroup() }) {
                        Text("No")
                    }
                }
            )
        }

        // --- Discard edit confirmation dialog ---
        if (uiState.showDiscardEditDialog) {
            val groupName = uiState.editingGroup?.toName() ?: ""
            AlertDialog(
                onDismissRequest = { viewModel.cancelDiscardEdit() },
                title = { Text("Discard changes?") },
                text = { Text("Discard changes to \"$groupName\"?") },
                confirmButton = {
                    Button(onClick = { viewModel.confirmDiscardEdit() }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDiscardEdit() }) {
                        Text("No")
                    }
                }
            )
        }
    }
}
