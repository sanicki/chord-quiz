package com.chordquiz.app.ui.screen.quizdraw

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.ui.components.QuizFeedbackBanner
import com.chordquiz.app.ui.components.QuizProgressBar
import com.chordquiz.app.ui.components.QuizTopBar
import com.chordquiz.app.ui.components.chord.ChordDiagram
import com.chordquiz.app.ui.components.chord.InteractiveChordDiagram
import com.chordquiz.app.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawQuizScreen(
    instrumentId: String,
    selectedChordIds: List<String>,
    questionCount: Int,
    repeatMissed: Boolean,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    viewModel: DrawQuizViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

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
            val chordQuestion = question as? QuizQuestion.ChordQuestion ?: return
            val stringCount = session.instrument.stringCount
            val autoContinueDelayMs = settings.autoContinueDelaySeconds * 1000

            // Swipe-to-submit flash state
            var showSwipeFlash by remember { mutableStateOf(false) }
            val flashAlpha by animateFloatAsState(
                targetValue = if (showSwipeFlash) 0.4f else 0f,
                animationSpec = tween(
                    durationMillis = if (showSwipeFlash) 40 else 160,
                    easing = LinearEasing
                ),
                label = "swipeFlash"
            )
            LaunchedEffect(showSwipeFlash) {
                if (showSwipeFlash) {
                    delay(80)
                    showSwipeFlash = false
                }
            }

            // Trigger haptic feedback on wrong answer
            LaunchedEffect(state.feedback) {
                if (state.feedback == AnswerFeedback.INCORRECT && settings.hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            // Auto-advance after delay when feedback is shown
            LaunchedEffect(state.feedback) {
                if (state.feedback != null) {
                    delay(autoContinueDelayMs.toLong())
                    viewModel.nextQuestion()
                }
            }

            val countdownProgress by animateFloatAsState(
                targetValue = if (state.feedback != null) 0f else 1f,
                animationSpec = if (state.feedback != null)
                    tween(durationMillis = autoContinueDelayMs, easing = LinearEasing)
                else
                    snap(),
                label = "countdown"
            )

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

                    Text("Draw this chord:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = chordQuestion.chordDefinition.displayName(settings.noteDisplayMode),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reset the interactive diagram when the question changes
                    key(state.displayedQuestionIndex) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(280.dp)
                                .pointerInput(state.feedback == null) {
                                    // Only active pre-feedback; coroutine is cancelled and re-launched
                                    // when state.feedback changes, so no double-submit is possible
                                    if (state.feedback != null) return@pointerInput

                                    var totalDragX = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDragX = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            totalDragX += dragAmount
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            // PointerInputScope implements Density, so dp.toPx() works directly
                                            if (totalDragX > 80.dp.toPx()) {
                                                showSwipeFlash = true
                                                viewModel.skipQuestion()
                                            }
                                            totalDragX = 0f
                                        },
                                        onDragCancel = { totalDragX = 0f }
                                    )
                                }
                                .drawWithContent {
                                    drawContent()
                                    // White wash overlay — alpha = 0 when not flashing (invisible but always present)
                                    drawRect(Color.White, size = Size(size.width, size.height), alpha = flashAlpha)
                                }
                        ) {
                            InteractiveChordDiagram(
                                stringCount = stringCount,
                                totalFrets = session.instrument.totalFrets,
                                initialFingering = state.currentFingering,
                                incorrectFrettedStrings = state.incorrectFrettedStrings,
                                incorrectMutedStrings = state.incorrectMutedStrings,
                                missedMuteStrings = state.missedMuteStrings,
                                openStringNotes = session.instrument.openStringNotes,
                                openStringOctaves = session.instrument.openStringOctaves,
                                noteDisplayMode = settings.noteDisplayMode,
                                onFingeringChanged = { viewModel.onFingeringChanged(it) },
                                onNoteSelected = { strIdx, fret -> viewModel.onNoteSelected(strIdx, fret) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        "Tap strings/frets to place fingers\nTap above nut to toggle open/muted\nDrag right-to-left along a fret to draw a barre\nStrum from left-to-right to submit answer",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.feedback != null) {
                        LinearProgressIndicator(
                            progress = { countdownProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Feedback banner
                    val isCorrect = state.feedback == AnswerFeedback.CORRECT
                    QuizFeedbackBanner(
                        isCorrect = isCorrect,
                        message = if (isCorrect) "Correct!" else "Not quite!",
                        visible = state.feedback != null,
                        animate = false,
                        content = if (!isCorrect) {
                            {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Not quite!",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("Correct fingering:", style = MaterialTheme.typography.bodySmall)
                                    ChordDiagram(
                                        chord = chordQuestion.chordDefinition,
                                        modifier = Modifier
                                            .size(width = 120.dp, height = 150.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        } else null
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
