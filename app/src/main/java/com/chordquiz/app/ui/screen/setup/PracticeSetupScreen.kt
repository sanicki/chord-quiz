package com.chordquiz.app.ui.screen.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.ui.components.PracticeSetupContent

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PracticeSetupContent(
                headerText = {
                    Text(
                        "${selectedChordIds.size} chords selected",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                drawModeDescription = "Place finger dots on a blank diagram",
                playModeDescription = "Strum your instrument, we'll detect the chord",
                repeatMissedLabel = "Repeat missed chords",
                repeatMissedDescription = "Replay incorrectly answered chords",
                currentMode = uiState.mode,
                onModeChanged = { viewModel.setMode(it) },
                questionCount = uiState.questionCount,
                onIncrementQuestionCount = { viewModel.incrementQuestionCount() },
                onDecrementQuestionCount = { viewModel.decrementQuestionCount() },
                repeatMissed = uiState.repeatMissed,
                onRepeatMissedChanged = { viewModel.setRepeatMissed(it) },
                onStartQuiz = {
                    if (uiState.mode == QuizMode.DRAW) {
                        onStartDrawQuiz(instrumentId, selectedChordIds,
                            uiState.questionCount, uiState.repeatMissed)
                    } else {
                        onStartPlayQuiz(instrumentId, selectedChordIds,
                            uiState.questionCount, uiState.repeatMissed)
                    }
                },
                drawModeValue = QuizMode.DRAW,
                playModeValue = QuizMode.PLAY
            )
        }
    }
}
