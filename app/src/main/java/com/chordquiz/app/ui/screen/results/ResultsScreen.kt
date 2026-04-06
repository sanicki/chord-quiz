package com.chordquiz.app.ui.screen.results

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.ui.components.chord.ChordDiagram
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.IncorrectRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    sessionId: String,
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onRestartQuiz: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }

    if (uiState.isLoading || uiState.session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = uiState.session!!
    val total = session.questions.size
    val correct = session.score
    val pct = if (total > 0) (correct * 100) / total else 0

    val stars = when {
        pct >= 90 -> "⭐⭐⭐"
        pct >= 70 -> "⭐⭐"
        pct >= 50 -> "⭐"
        else -> ""
    }

    val missedChords = session.answers
        .filter { !it.isCorrect }
        .distinctBy { it.question.chordDefinition.id }
        .sortedBy { it.question.chordDefinition.chordName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, "Home")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stars.isNotEmpty()) {
                Text(stars, fontSize = 48.sp, textAlign = TextAlign.Center)
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$correct / $total",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$pct% Correct",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Answer summary chips
            Text("REVIEW", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth())
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(session.answers) { answer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (answer.isCorrect) Icons.Default.CheckCircle
                                          else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (answer.isCorrect) CorrectGreen else IncorrectRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            " ${answer.question.chordDefinition.chordName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Missed chords section
            if (missedChords.isNotEmpty()) {
                Text("MISSED CHORDS", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth())
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(missedChords) { answer ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ChordDiagram(
                                chord = answer.question.chordDefinition,
                                modifier = Modifier.size(width = 80.dp, height = 100.dp)
                            )
                            Text(
                                answer.question.chordDefinition.chordName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestartQuiz,
                    modifier = Modifier.weight(1f)
                ) { Text("Try Again") }

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) { Text("Back") }
            }
        }
    }
}
