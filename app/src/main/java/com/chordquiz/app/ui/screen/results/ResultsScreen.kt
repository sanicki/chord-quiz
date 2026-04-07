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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.ui.components.chord.ChordDiagram
import com.chordquiz.app.ui.components.staff.Clef
import com.chordquiz.app.ui.components.staff.MusicStaff
import com.chordquiz.app.ui.components.staff.MusicalGradeStaff
import com.chordquiz.app.ui.components.staff.StaffNote
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

    val isNoteSession = session.mode == QuizMode.NOTE_DRAW || session.mode == QuizMode.NOTE_PLAY

    val missedChords = if (!isNoteSession) {
        session.answers
            .filter { !it.isCorrect }
            .filter { it.question is QuizQuestion.ChordQuestion }
            .distinctBy { (it.question as QuizQuestion.ChordQuestion).chordDefinition.id }
            .sortedBy { (it.question as QuizQuestion.ChordQuestion).chordDefinition.chordName }
    } else emptyList()

    val missedNotes = if (isNoteSession) {
        session.answers
            .filter { !it.isCorrect }
            .filter { it.question is QuizQuestion.NoteQuestion }
            .map { it.question as QuizQuestion.NoteQuestion }
            .distinctBy { it.displayName }
            .sortedWith(compareBy({ it.octave }, { it.note.semitone }))
            .map { StaffNote(semitone = it.note.semitone, octave = it.octave, displayName = it.displayName) }
    } else emptyList()

    val clef = if (session.instrument.id == "bass_standard") Clef.BASS else Clef.TREBLE

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
            // Musical grade staff — replaces the old star rating
            val gradeFeedback = if (pct == 100) "Perfect! 🎉" else null
            MusicalGradeStaff(
                scorePercent = pct,
                instrumentId = session.instrument.id,
                feedback = gradeFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            // Score card
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
            Text(
                "REVIEW",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(session.answers) { answer ->
                    val label = when (val q = answer.question) {
                        is QuizQuestion.ChordQuestion -> q.chordDefinition.chordName
                        is QuizQuestion.NoteQuestion  -> q.displayName
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (answer.isCorrect) Icons.Default.CheckCircle
                                          else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (answer.isCorrect) CorrectGreen else IncorrectRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(" $label", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Missed chords section
            if (missedChords.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "MISSED CHORDS",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(missedChords) { answer ->
                                val chordQuestion = answer.question as QuizQuestion.ChordQuestion
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ChordDiagram(
                                        chord = chordQuestion.chordDefinition,
                                        modifier = Modifier.size(width = 80.dp, height = 100.dp)
                                    )
                                    Text(
                                        chordQuestion.chordDefinition.chordName,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Missed notes section
            if (missedNotes.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "MISSED NOTES",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        MusicStaff(
                            notes = missedNotes,
                            clef = clef,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
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
