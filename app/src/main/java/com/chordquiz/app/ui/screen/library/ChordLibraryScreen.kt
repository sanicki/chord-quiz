package com.chordquiz.app.ui.screen.library

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.ui.components.chord.ChordDiagram

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

    LaunchedEffect(instrumentId) {
        viewModel.loadInstrument(instrumentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.instrument?.displayName ?: "Chords") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedChordIds.size >= 2) {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save as Group")
                        }
                        Button(
                            onClick = {
                                onStartPractice(instrumentId, uiState.selectedChordIds.toList())
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Start →")
                        }
                    }
                }
            )
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
                    // All
                    FilterChip(
                        selected = uiState.activeTypeFilter == null && uiState.activeGroupFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("All") }
                    )
                    // Custom groups (newest first)
                    uiState.customGroups.forEach { group ->
                        FilterChip(
                            selected = uiState.activeGroupFilter?.id == group.id,
                            onClick = { viewModel.setGroupFilter(group) },
                            label = { Text(group.toName()) }
                        )
                    }
                    // Preset type filters
                    ChordType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.activeTypeFilter == type,
                            onClick = { viewModel.setTypeFilter(type) },
                            label = { Text(type.displayName) }
                        )
                    }
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

        // Save dialog
        if (showSaveDialog) {
            var groupName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showSaveDialog = false }) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("Save as New Group", style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            label = { Text("Group name") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.saveGroup(groupName, instrumentId, uiState.selectedChordIds.toList())
                                    showSaveDialog = false
                                },
                                enabled = groupName.isNotBlank() && uiState.selectedChordIds.size >= 2
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}
