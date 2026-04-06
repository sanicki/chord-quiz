package com.chordquiz.app.ui.screen.quizplay

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.ui.components.AudioWaveform
import com.chordquiz.app.ui.components.chord.ChordDiagram
import com.chordquiz.app.ui.screen.settings.SettingsViewModel
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.IncorrectRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQuizScreen(
    instrumentId: String,
    selectedChordIds: List<String>,
    questionCount: Int,
    repeatMissed: Boolean,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    viewModel: PlayQuizViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            viewModel.initialize(instrumentId, selectedChordIds, questionCount, repeatMissed)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    when (val state = uiState) {
        is PlayQuizUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Requesting microphone access...")
                }
            }
        }

        is PlayQuizUiState.Complete -> {
            LaunchedEffect(state.sessionId) { onQuizComplete(state.sessionId) }
        }

        is PlayQuizUiState.Active -> {
            val session = state.session
            val question = session.currentQuestion ?: return

            val amplitudeAnim by animateFloatAsState(
                targetValue = state.amplitude,
                animationSpec = tween(100),
                label = "amplitude"
            )

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Question ${session.currentIndex + 1} / ${session.questions.size}")
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.Close, "Exit quiz")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.skipQuestion() }) {
                                Icon(Icons.Default.SkipNext, "Skip")
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
                        progress = { session.currentIndex.toFloat() / session.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Play this chord:", style = MaterialTheme.typography.bodyLarge)

                    Text(
                        text = question.chordDefinition.displayName(settings.noteDisplayMode),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reference chord diagram
                    ChordDiagram(
                        chord = question.chordDefinition,
                        modifier = Modifier
                            .width(180.dp)
                            .height(220.dp)
                    )

                    // Audio feedback area
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (state.isListening) "🎙 Listening..." else "✓ Got it",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            AudioWaveform(
                                amplitude = amplitudeAnim,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (state.detectedNotes.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Detected: ${state.detectedNotes.joinToString(" ") { it.displayNameFor(settings.noteDisplayMode) }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Feedback banner
                    AnimatedVisibility(
                        visible = state.feedback != null,
                        enter = fadeIn() + scaleIn()
                    ) {
                        val feedback = state.feedback
                        val isCorrect = feedback?.isCorrect == true
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCorrect) CorrectGreen.copy(alpha = 0.15f)
                                    else IncorrectRed.copy(alpha = 0.15f),
                                    MaterialTheme.shapes.medium
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (feedback) {
                                    PlayFeedback.CORRECT_PERFECT -> "✓ Perfect!"
                                    PlayFeedback.CORRECT_GOOD -> "✓ Good!"
                                    PlayFeedback.CORRECT_CLOSE -> "✓ Close enough!"
                                    PlayFeedback.INCORRECT -> "✗ Skipped"
                                    null -> ""
                                },
                                color = if (isCorrect) CorrectGreen else IncorrectRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (state.feedback != null) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isLast = session.currentIndex + 1 >= session.questions.size
                            Text(if (isLast) "Finish" else "Next  →")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.skipQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip  ⏭")
                        }
                    }
                }
            }
        }
    }
}
