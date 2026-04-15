package com.chordquiz.app.ui.screen.notedrawquiz

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.ui.components.QuizFeedbackBanner
import com.chordquiz.app.ui.components.QuizProgressBar
import com.chordquiz.app.ui.components.QuizTopBar
import com.chordquiz.app.ui.components.chord.InteractiveChordDiagram
import com.chordquiz.app.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDrawQuizScreen(
    instrumentId: String,
    noteMode: NoteMode,
    questionCount: Int,
    repeatMissed: Boolean,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    viewModel: NoteDrawQuizViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(instrumentId) {
        viewModel.initialize(instrumentId, noteMode, questionCount, repeatMissed)
    }

    when (val state = uiState) {
        is NoteDrawQuizUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NoteDrawQuizUiState.Complete -> {
            LaunchedEffect(state.sessionId) { onQuizComplete(state.sessionId) }
        }
        is NoteDrawQuizUiState.Active -> {
            val session = state.session
            val question = state.displayedQuestion ?: session.currentQuestion ?: return
            val noteQuestion = question as? QuizQuestion.NoteQuestion ?: return
            val stringCount = session.instrument.stringCount
            val autoContinueDelayMs = settings.autoContinueDelaySeconds * 1000

            // Auto-advance after delay when correct feedback is shown
            // (FIND_ALL wrong feedback clears itself without advancing)
            LaunchedEffect(state.feedback) {
                if (state.feedback == NoteDrawFeedback.CORRECT) {
                    delay(autoContinueDelayMs.toLong())
                    viewModel.nextQuestion()
                } else if (state.feedback == NoteDrawFeedback.INCORRECT &&
                    (noteQuestion.noteMode == NoteMode.FIND_NOTE ||
                     noteQuestion.noteMode == NoteMode.FIND_NOTE_CORRECT_OCTAVE)
                ) {
                    // Single-note wrong: auto-advance after same delay
                    delay(autoContinueDelayMs.toLong())
                    viewModel.nextQuestion()
                }
            }

            val countdownProgress by animateFloatAsState(
                targetValue = if (state.feedback != null &&
                    (state.feedback == NoteDrawFeedback.CORRECT ||
                     noteQuestion.noteMode == NoteMode.FIND_NOTE ||
                     noteQuestion.noteMode == NoteMode.FIND_NOTE_CORRECT_OCTAVE)
                ) 0f else 1f,
                animationSpec = if (state.feedback != null &&
                    (state.feedback == NoteDrawFeedback.CORRECT ||
                     noteQuestion.noteMode == NoteMode.FIND_NOTE ||
                     noteQuestion.noteMode == NoteMode.FIND_NOTE_CORRECT_OCTAVE)
                )
                    tween(durationMillis = autoContinueDelayMs, easing = LinearEasing)
                else
                    snap(),
                label = "countdown"
            )

            val promptText = when (noteQuestion.noteMode) {
                NoteMode.FIND_NOTE, NoteMode.FIND_ALL_NOTES -> noteQuestion.displayName
                NoteMode.FIND_NOTE_CORRECT_OCTAVE, NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE ->
                    "${noteQuestion.displayName}${noteQuestion.octave}"
            }

            val instructionText = when (noteQuestion.noteMode) {
                NoteMode.FIND_NOTE -> "Tap any position for this note"
                NoteMode.FIND_NOTE_CORRECT_OCTAVE -> "Tap this exact note and octave"
                NoteMode.FIND_ALL_NOTES -> "Tap all positions for this note"
                NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE -> "Tap all positions for this exact octave"
            }

            // Show countdown only when auto-advancing (correct or single-note wrong)
            val showCountdown = state.feedback != null &&
                (state.feedback == NoteDrawFeedback.CORRECT ||
                 noteQuestion.noteMode == NoteMode.FIND_NOTE ||
                 noteQuestion.noteMode == NoteMode.FIND_NOTE_CORRECT_OCTAVE)

            Scaffold(
                topBar = {
                    QuizTopBar(
                        currentIndex = state.displayedQuestionIndex,
                        totalCount = session.questions.size,
                        onBack = onNavigateBack
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
                    QuizProgressBar(
                        currentIndex = state.displayedQuestionIndex,
                        totalCount = session.questions.size
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(instructionText, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    key(state.displayedQuestionIndex, state.wrongTapResetKey) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(280.dp)
                        ) {
                            InteractiveChordDiagram(
                                stringCount = stringCount,
                                totalFrets = session.instrument.totalFrets,
                                initialFingering = state.currentFingering,
                                noteQuizMode = true,
                                hintPositions = state.hintPositions,
                                incorrectFrettedStrings = emptySet(),
                                incorrectMutedStrings = emptySet(),
                                missedMuteStrings = emptySet(),
                                openStringNotes = session.instrument.openStringNotes,
                                openStringOctaves = session.instrument.openStringOctaves,
                                noteDisplayMode = settings.noteDisplayMode,
                                onFingeringChanged = { viewModel.onFingeringChanged(it) },
                                onNoteSelected = { stringIndex, fret ->
                                    viewModel.onNoteSelected(stringIndex, fret)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        "Tap strings/frets to mark notes",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showCountdown) {
                        LinearProgressIndicator(
                            progress = { countdownProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Feedback banner
                    QuizFeedbackBanner(
                        isCorrect = state.feedback == NoteDrawFeedback.CORRECT,
                        message = if (state.feedback == NoteDrawFeedback.CORRECT) "Correct!" else "Not quite!",
                        visible = state.feedback != null,
                        animate = false
                    )

                    Spacer(Modifier.weight(1f))

                    if (state.feedback == null) {
                        Button(
                            onClick = { viewModel.skipQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip")
                        }
                    }
                }
            }
        }
    }
}
