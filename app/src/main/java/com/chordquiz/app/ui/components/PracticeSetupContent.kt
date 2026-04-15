package com.chordquiz.app.ui.components

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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chordquiz.app.data.model.QuizMode

@Composable
fun PracticeSetupContent(
    headerText: @Composable () -> Unit,
    drawModeTitle: String = "Draw",
    drawModeDescription: String,
    playModeTitle: String = "Play",
    playModeDescription: String,
    repeatMissedLabel: String,
    repeatMissedDescription: String,
    currentMode: QuizMode,
    onModeChanged: (QuizMode) -> Unit,
    questionCount: Int,
    onIncrementQuestionCount: () -> Unit,
    onDecrementQuestionCount: () -> Unit,
    repeatMissed: Boolean,
    onRepeatMissedChanged: (Boolean) -> Unit,
    onStartQuiz: () -> Unit,
    drawModeValue: QuizMode,
    playModeValue: QuizMode
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        headerText()

        // Quiz Mode
        Text("QUIZ MODE", style = MaterialTheme.typography.labelLarge)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (currentMode == drawModeValue)
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
                    Text(drawModeTitle, style = MaterialTheme.typography.titleSmall)
                    Text(drawModeDescription,
                        style = MaterialTheme.typography.bodySmall)
                }
                RadioButton(
                    selected = currentMode == drawModeValue,
                    onClick = { onModeChanged(drawModeValue) }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (currentMode == playModeValue)
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
                    Text(playModeTitle, style = MaterialTheme.typography.titleSmall)
                    Text(playModeDescription,
                        style = MaterialTheme.typography.bodySmall)
                }
                RadioButton(
                    selected = currentMode == playModeValue,
                    onClick = { onModeChanged(playModeValue) }
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
            IconButton(onClick = onDecrementQuestionCount) {
                Icon(Icons.Default.RemoveCircle, "Decrease")
            }
            Text(
                questionCount.toString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            IconButton(onClick = onIncrementQuestionCount) {
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
                Text(repeatMissedLabel, style = MaterialTheme.typography.titleSmall)
                Text(repeatMissedDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = repeatMissed,
                onCheckedChange = { onRepeatMissedChanged(it) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStartQuiz,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Quiz  →")
        }
    }
}
