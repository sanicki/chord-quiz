package com.chordquiz.app.ui.screen.strumpractice

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.db.entity.SavedPatternEntity
import com.chordquiz.app.ui.components.strum.StrumNoteSymbol
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(0.dp))

            // ── BPM control ──────────────────────────────────────────────────
            BpmControl(
                bpm = uiState.bpm,
                onMinus = { viewModel.adjustBpm(-1) },
                onPlus = { viewModel.adjustBpm(+1) }
            )

            // ── Speed control ────────────────────────────────────────────────
            SpeedControl(
                speedPercent = uiState.speedPercent,
                onMinus = { viewModel.adjustSpeedPercent(-5) },
                onPlus = { viewModel.adjustSpeedPercent(+5) }
            )

            // ── Note type selector ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Note Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StrumNote.entries.forEach { note ->
                        OutlinedButton(
                            onClick = { viewModel.onNoteTypeChanged(note) }
                        ) {
                            StrumNoteSymbol(noteType = note)
                        }
                    }
                }
            }

            // ── Pattern grid ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pattern", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth()) {
                    uiState.slots.forEachIndexed { index, slot ->
                        SlotBox(
                            slot = slot,
                            label = uiState.noteType.slotLabels[index],
                            isActive = index == uiState.activeSlotIndex,
                            onTap = { viewModel.onSlotTapped(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Saved patterns FlowRow ───────────────────────────────────
                @OptIn(ExperimentalFoundationApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Save button (always first)
                    OutlinedButton(
                        onClick = { viewModel.showSaveDialog() }
                    ) {
                        Text("Save")
                    }
                    // Saved pattern buttons
                    uiState.savedPatterns.forEach { pattern ->
                        OutlinedButton(
                            onClick = { /* disabled - handled by combinedClickable */ },
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.loadPattern(pattern) },
                                onLongClick = { viewModel.requestDeletePattern(pattern) }
                            )
                        ) {
                            Text(pattern.toName())
                        }
                    }
                }
            }

            // ── Play / Stop button ───────────────────────────────────────────
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

            Spacer(Modifier.height(0.dp))
        }
    }

    // ── Save dialog ──────────────────────────────────────────────────────────
    if (uiState.showSaveDialog) {
        SavePatternDialog(
            nameError = uiState.saveNameError,
            onSave = { name ->
                viewModel.requestSavePattern(name)
            },
            onDismiss = { viewModel.dismissSaveDialog() },
            onClearError = { viewModel.clearSaveNameError() }
        )
    }

    // ── Replace dialog ───────────────────────────────────────────────────────
    if (uiState.showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelReplacePattern() },
            title = { Text("Replace Pattern?") },
            text = {
                Text("A pattern named \"${uiState.replacePatternName}\" already exists. Replace it?")
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmReplacePattern() }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelReplacePattern() }) { Text("No") }
            }
        )
    }

    // ── Delete confirmation dialog ───────────────────────────────────────────
    uiState.deleteConfirmPattern?.let { pattern ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeletePattern() },
            title = { Text("Delete pattern ${pattern.toName()}?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmDeletePattern() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeletePattern() }) { Text("Cancel") }
            }
        )
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun BpmControl(bpm: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tempo", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMinus, enabled = bpm > 40) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease BPM")
            }
            Text(
                text = "$bpm BPM",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPlus, enabled = bpm < 240) {
                Icon(Icons.Default.Add, contentDescription = "Increase BPM")
            }
        }
    }
}

@Composable
private fun SpeedControl(speedPercent: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Speed", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMinus, enabled = speedPercent > 1) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease speed")
            }
            Text(
                text = "$speedPercent% Speed",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPlus, enabled = speedPercent < 100) {
                Icon(Icons.Default.Add, contentDescription = "Increase speed")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotBox(
    slot: StrumSlot,
    label: String,
    isActive: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor   = MaterialTheme.colorScheme.primary
    val borderColor    = if (isActive) primaryColor else MaterialTheme.colorScheme.outline
    val bgColor        = if (isActive) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val contentColor   = when {
        isActive                  -> primaryColor
        slot == StrumSlot.MUTE    -> MaterialTheme.colorScheme.onSurfaceVariant
        else                      -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .background(bgColor)
            .border(width = if (isActive) 2.dp else 1.dp, color = borderColor)
            .combinedClickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (slot) {
                StrumSlot.DOWN -> "D"
                StrumSlot.UP   -> "U"
                StrumSlot.MUTE -> label
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun SavePatternDialog(
    nameError: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester     = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Pattern") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onClearError() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("Pattern name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = if (nameError != null) ({ Text(nameError) }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) })
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
