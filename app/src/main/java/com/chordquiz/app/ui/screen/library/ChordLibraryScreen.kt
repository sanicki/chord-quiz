package com.chordquiz.app.ui.screen.library

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.ChordType
import com.chordquiz.app.ui.components.chord.ChordDiagram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordLibraryScreen(
    instrumentId: String,
    onNavigateBack: () -> Unit,
    onStartPractice: (String, List<String>) -> Unit,
    onNavigateToGroups: (String) -> Unit,
    onSaveGroup: (String, List<String>) -> Unit,
    viewModel: ChordLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    IconButton(
                        onClick = { onNavigateToGroups(instrumentId) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Filled.List, contentDescription = "Groups")
                    }
                    if (uiState.selectedChordIds.isNotEmpty()) {
                        IconButton(
                            onClick = { onSaveGroup(instrumentId, uiState.selectedChordIds.toList()) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save as Group")
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
                // Selection info + Select All
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

                // Type filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.activeTypeFilter == null,
                            onClick = { viewModel.setTypeFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(ChordType.entries.toList()) { type ->
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
    }
}
