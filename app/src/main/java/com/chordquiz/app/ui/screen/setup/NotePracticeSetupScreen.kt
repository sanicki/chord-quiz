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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.ui.components.PracticeSetupContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePracticeSetupScreen(
    instrumentId: String,
    noteMode: NoteMode,
    onNavigateBack: () -> Unit,
    onStartDrawQuiz: (String, NoteMode, Int, Boolean) -> Unit,
    onStartPlayQuiz: (String, NoteMode, Int, Boolean) -> Unit,
    viewModel: NotePracticeSetupViewModel = hiltViewModel()
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
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(noteMode.displayName)
                            }
                            append(" selected")
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                drawModeDescription = "Place finger dots on the notes",
                playModeDescription = "Pluck your instrument, we'll detect the note",
                repeatMissedLabel = "Repeat missed notes",
                repeatMissedDescription = "Replay incorrectly answered notes",
                currentMode = uiState.mode,
                onModeChanged = { viewModel.setMode(it) },
                questionCount = uiState.questionCount,
                onIncrementQuestionCount = { viewModel.incrementQuestionCount() },
                onDecrementQuestionCount = { viewModel.decrementQuestionCount() },
                repeatMissed = uiState.repeatMissed,
                onRepeatMissedChanged = { viewModel.setRepeatMissed(it) },
                onStartQuiz = {
                    if (uiState.mode == QuizMode.NOTE_DRAW) {
                        onStartDrawQuiz(instrumentId, noteMode,
                            uiState.questionCount, uiState.repeatMissed)
                    } else {
                        onStartPlayQuiz(instrumentId, noteMode,
                            uiState.questionCount, uiState.repeatMissed)
                    }
                },
                drawModeValue = QuizMode.NOTE_DRAW,
                playModeValue = QuizMode.NOTE_PLAY
            )
        }
    }
}
