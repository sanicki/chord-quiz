package com.chordquiz.app.ui.screen.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.QuizMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSetupScreen(
    instrumentId: String,
    selectedChordIds: List<String>,
    onNavigateBack: () -> Unit,
    onStartDrawQuiz: (String, List<String>, Int, Boolean) -> Unit,
    onStartPlayQuiz: (String, List<String>, Int, Boolean) -> Unit,
    viewModel: PracticeSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "${selectedChordIds.size} chords selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quiz Mode
            Text("QUIZ MODE", style = MaterialTheme.typography.labelLarge)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.mode == QuizMode.DRAW)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Draw", style = MaterialTheme.typography.titleSmall)
                        Text("Place finger dots on a blank diagram",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(
                        selected = uiState.mode == QuizMode.DRAW,
                        onClick = { viewModel.setMode(QuizMode.DRAW) }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.mode == QuizMode.PLAY)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Play", style = MaterialTheme.typography.titleSmall)
                        Text("Strum your instrument, we'll detect the chord",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(
                        selected = uiState.mode == QuizMode.PLAY,
                        onClick = { viewModel.setMode(QuizMode.PLAY) }
                    )
                }
            }

            HorizontalDivider()

            // Question count
            Text("NUMBER OF QUESTIONS", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.decrementQuestionCount() }) {
                    Icon(Icons.Default.RemoveCircle, "Decrease")
                }
                Text(
                    uiState.questionCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                IconButton(onClick = { viewModel.incrementQuestionCount() }) {
                    Icon(Icons.Default.AddCircle, "Increase")
                }
            }

            HorizontalDivider()

            // Repeat missed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Repeat missed chords", style = MaterialTheme.typography.titleSmall)
                    Text("Replay incorrectly answered chords",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.repeatMissed,
                    onCheckedChange = { viewModel.setRepeatMissed(it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (uiState.mode == QuizMode.DRAW) {
                        onStartDrawQuiz(instrumentId, selectedChordIds,
                            uiState.questionCount, uiState.repeatMissed)
                    } else {
                        onStartPlayQuiz(instrumentId, selectedChordIds,
                            uiState.questionCount, uiState.repeatMissed)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Quiz  →")
            }
        }
    }
}
