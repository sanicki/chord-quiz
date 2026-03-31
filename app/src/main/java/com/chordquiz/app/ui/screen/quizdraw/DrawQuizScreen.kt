package com.chordquiz.app.ui.screen.quizdraw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.ui.components.chord.ChordDiagram
import com.chordquiz.app.ui.components.chord.InteractiveChordDiagram
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.IncorrectRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawQuizScreen(
    instrumentId: String,
    selectedChordIds: List<String>,
    questionCount: Int,
    repeatMissed: Boolean,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    viewModel: DrawQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(instrumentId) {
        viewModel.initialize(instrumentId, selectedChordIds, questionCount, repeatMissed)
    }

    when (val state = uiState) {
        is DrawQuizUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DrawQuizUiState.Complete -> {
            LaunchedEffect(state.sessionId) { onQuizComplete(state.sessionId) }
        }
        is DrawQuizUiState.Active -> {
            val session = state.session
            // Use displayedQuestion so the chord name doesn't change until Next is pressed
            val question = state.displayedQuestion ?: session.currentQuestion ?: return
            val stringCount = session.instrument.stringCount

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Question ${state.displayedQuestionIndex + 1} / ${session.questions.size}")
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.Close, "Exit quiz")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { state.displayedQuestionIndex.toFloat() / session.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Draw this chord:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = question.chordDefinition.chordName,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reset the interactive diagram when the question changes
                    key(state.displayedQuestionIndex) {
                        InteractiveChordDiagram(
                            stringCount = stringCount,
                            initialFingering = state.currentFingering,
                            isFeedbackIncorrect = state.feedback == AnswerFeedback.INCORRECT,
                            onFingeringChanged = { viewModel.onFingeringChanged(it) },
                            onNoteSelected = { strIdx, fret -> viewModel.onNoteSelected(strIdx, fret) },
                            modifier = Modifier
                                .width(220.dp)
                                .height(280.dp)
                        )
                    }

                    Text(
                        "Tap strings/frets to place fingers\nTap above nut to toggle open/muted",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Feedback banner
                    AnimatedVisibility(
                        visible = state.feedback != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val isCorrect = state.feedback == AnswerFeedback.CORRECT
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCorrect) CorrectGreen.copy(alpha = 0.15f)
                                    else IncorrectRed.copy(alpha = 0.15f),
                                    MaterialTheme.shapes.medium
                                )
                                .padding(12.dp)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isCorrect) "✓ Correct!" else "✗ Not quite!",
                                    color = if (isCorrect) CorrectGreen else IncorrectRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isCorrect) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Correct fingering:", style = MaterialTheme.typography.bodySmall)
                                    ChordDiagram(
                                        chord = question.chordDefinition,
                                        modifier = Modifier
                                            .size(width = 120.dp, height = 150.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (state.feedback == null) {
                        Button(
                            onClick = { viewModel.submitAnswer() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit  ✓")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isLast = state.displayedQuestionIndex + 1 >= session.questions.size
                            Text(if (isLast) "Finish" else "Next  →")
                        }
                    }
                }
            }
        }
    }
}
