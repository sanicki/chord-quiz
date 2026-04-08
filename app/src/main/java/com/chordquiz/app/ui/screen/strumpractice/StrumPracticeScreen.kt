package com.chordquiz.app.ui.screen.strumpractice

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrumPracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: StrumPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Strum Practice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(0.dp))

            // BPM control
            BpmControl(
                bpm = uiState.bpm,
                speedPercent = uiState.speedPercent,
                effectiveBpm = uiState.effectiveBpm,
                onBpmDelta = viewModel::adjustBpm,
                onSpeedDelta = viewModel::adjustSpeedPercent
            )

            // Note type chips
            NoteTypeSelector(
                selected = uiState.noteType,
                onSelect = viewModel::onNoteTypeChanged
            )

            // Strumming grid
            StrumGrid(
                slots = uiState.slots,
                activeSlotIndex = uiState.activeSlotIndex,
                onSlotTapped = viewModel::onSlotTapped
            )

            // Play/Stop button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = viewModel::togglePlay,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BpmControl(
    bpm: Int,
    speedPercent: Int,
    effectiveBpm: Int,
    onBpmDelta: (Int) -> Unit,
    onSpeedDelta: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tempo", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onBpmDelta(-5) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("-5")
            }
            OutlinedButton(onClick = { onBpmDelta(-1) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("-1")
            }
            Text(
                text = "$bpm BPM",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = { onBpmDelta(1) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+1")
            }
            OutlinedButton(onClick = { onBpmDelta(5) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+5")
            }
        }

        // Speed control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onSpeedDelta(-5) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("-5%")
            }
            Text(
                text = "Speed: $speedPercent% (${effectiveBpm} BPM)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = { onSpeedDelta(5) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+5%")
            }
        }
    }
}

@Composable
private fun NoteTypeSelector(
    selected: StrumNote,
    onSelect: (StrumNote) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Note Type", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StrumNote.entries.forEach { note ->
                FilterChip(
                    selected = note == selected,
                    onClick = { onSelect(note) },
                    label = { Text(note.label) }
                )
            }
        }
    }
}

@Composable
private fun StrumGrid(
    slots: List<StrumSlot>,
    activeSlotIndex: Int,
    onSlotTapped: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to keep active slot visible
    LaunchedEffect(activeSlotIndex) {
        if (activeSlotIndex >= 0) {
            listState.animateScrollToItem(activeSlotIndex)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pattern", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(slots) { index, slot ->
                SlotBox(
                    slot = slot,
                    isActive = index == activeSlotIndex,
                    onTap = { onSlotTapped(index) }
                )
            }
        }
    }
}

@Composable
private fun SlotBox(
    slot: StrumSlot,
    isActive: Boolean,
    onTap: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isActive) activeColor else MaterialTheme.colorScheme.outline
    val bgColor = if (isActive) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        when (slot) {
            StrumSlot.DOWN -> Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down strum",
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            StrumSlot.UP -> Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up strum",
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            StrumSlot.MUTE -> Text(
                text = "×",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
