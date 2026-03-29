package com.chordquiz.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated waveform visualizer for the Play Quiz screen.
 * [amplitude] from 0.0 to 1.0 controls wave height.
 */
@Composable
fun AudioWaveform(
    amplitude: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1976D2),
    barCount: Int = 40
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = i * (size.width / barCount) + barWidth / 2
            val sineVal = sin(i * (2f * PI / barCount) + phase).toFloat()
            val barHeight = (amplitude * size.height * 0.45f) * ((sineVal + 1f) / 2f + 0.1f)

            drawLine(
                color = color.copy(alpha = 0.7f + 0.3f * amplitude),
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth
            )
        }
    }
}
