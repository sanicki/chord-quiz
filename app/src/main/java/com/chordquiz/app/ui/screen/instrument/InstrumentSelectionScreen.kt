package com.chordquiz.app.ui.screen.instrument

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.ui.components.InstrumentCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InstrumentSelectionScreen(
    onChordInstrumentSelected: (String) -> Unit,
    onNoteInstrumentSelected: (String) -> Unit,
    onTunerInstrumentSelected: (String) -> Unit = {},
    onStrumPracticeSelected: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: InstrumentSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Transient selection state for Strum Practice chip — reset whenever the screen resumes
    var strumPracticeSelected by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) strumPracticeSelected = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chord Quiz") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
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
                // Quiz type chip selector — FlowRow in the body gives full screen width
                // and allows proper wrapping so all chips are always visible.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    QuizTypeButton(
                        label = "Chord Quiz",
                        isSelected = uiState.quizType == QuizType.CHORD,
                        onClick = { viewModel.onQuizTypeChanged(QuizType.CHORD) }
                    )
                    QuizTypeButton(
                        label = "Note Quiz",
                        isSelected = uiState.quizType == QuizType.NOTE,
                        onClick = { viewModel.onQuizTypeChanged(QuizType.NOTE) }
                    )
                    QuizTypeButton(
                        label = "Strum Practice",
                        isSelected = strumPracticeSelected,
                        onClick = {
                            strumPracticeSelected = true
                            onStrumPracticeSelected()
                        }
                    )
                    QuizTypeButton(
                        label = "Tuner",
                        isSelected = uiState.quizType == QuizType.TUNER,
                        onClick = { viewModel.onQuizTypeChanged(QuizType.TUNER) }
                    )
                }

                Text(
                    text = "Choose your instrument",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.instruments) { instrument ->
                        InstrumentCard(
                            instrument = instrument,
                            isSelected = false,
                            onClick = {
                                viewModel.onInstrumentSelected(instrument)
                                when (uiState.quizType) {
                                    QuizType.CHORD -> onChordInstrumentSelected(instrument.id)
                                    QuizType.NOTE -> onNoteInstrumentSelected(instrument.id)
                                    QuizType.TUNER -> onTunerInstrumentSelected(instrument.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}
