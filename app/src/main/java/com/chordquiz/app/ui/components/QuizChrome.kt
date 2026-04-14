package com.chordquiz.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.IncorrectRed

/**
 * Top app bar for quiz screens showing the current question number,
 * a back button, and an optional skip button.
 *
 * @param currentIndex The current question index (0-based)
 * @param totalCount The total number of questions
 * @param onBack Callback when the back button is pressed
 * @param onSkip Optional callback for a skip button. If null, the skip button is not shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTopBar(
    currentIndex: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onSkip: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Text("Question ${currentIndex + 1} / $totalCount")
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, "Exit quiz")
            }
        },
        actions = {
            if (onSkip != null) {
                IconButton(onClick = onSkip) {
                    Icon(Icons.Default.SkipNext, "Skip")
                }
            }
        }
    )
}

/**
 * Linear progress indicator for quiz screens showing the progress
 * through the questions.
 *
 * @param currentIndex The current question index (0-based)
 * @param totalCount The total number of questions
 */
@Composable
fun QuizProgressBar(currentIndex: Int, totalCount: Int) {
    LinearProgressIndicator(
        progress = { currentIndex.toFloat() / totalCount },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Animated feedback banner for quiz screens that displays whether an answer
 * was correct or incorrect with customizable content.
 *
 * @param isCorrect Whether the answer was correct (determines background color)
 * @param message The feedback message to display
 * @param visible Whether the banner should be visible
 * @param animate Whether to use scale-in animation (defaults to true)
 * @param content Optional composable content to display below the message (e.g., a diagram)
 */
@Composable
fun QuizFeedbackBanner(
    isCorrect: Boolean,
    message: String,
    visible: Boolean,
    animate: Boolean = true,
    content: @Composable (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = if (animate) fadeIn() + scaleIn() else fadeIn(),
        exit = fadeOut()
    ) {
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
            if (content != null) {
                // Content is provided; caller is responsible for layout
                content()
            } else {
                // Simple centered text
                Text(
                    text = message,
                    color = if (isCorrect) CorrectGreen else IncorrectRed,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
            }
        }
    }
}
