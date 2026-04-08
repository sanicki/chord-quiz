package com.chordquiz.app.ui.components.chord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.ui.theme.BarreColor
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown

/**
 * Displays a read-only chord diagram for a given [ChordDefinition].
 * Uses the first fingering by default.
 */
@Composable
fun ChordDiagram(
    chord: ChordDefinition,
    modifier: Modifier = Modifier,
    fingeringIndex: Int = 0,
    showChordName: Boolean = false
) {
    val fingering = chord.fingerings.getOrElse(fingeringIndex) { chord.fingerings.first() }
    ChordDiagramCanvas(
        fingering = fingering,
        stringCount = fingering.positions.size,
        modifier = modifier
    )
}

/**
 * Core Canvas-based chord diagram renderer.
 * Can be used for both static display and interactive overlays.
 *
 * The background is transparent — callers control the surface behind this composable.
 * All structural strokes (strings, fret wires) use [MaterialTheme.colorScheme.onSurface]
 * so they are legible in both light and dark mode.
 *
 * [stringLineColor] and [openColor] default to [Color.Unspecified], which causes the
 * implementation to fall back to [MaterialTheme.colorScheme.onSurface].  Pass explicit
 * values only when a specific override is needed.
 */
@Composable
fun ChordDiagramCanvas(
    fingering: Fingering,
    stringCount: Int,
    modifier: Modifier = Modifier,
    displayedFrets: Int = 5,
    dotColor: Color = FingerDot,
    stringLineColor: Color = Color.Unspecified,
    nutColor: Color = NutBrown,
    barreColor: Color = BarreColor,
    mutedColor: Color = MutedGray,
    openColor: Color = Color.Unspecified
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val effectiveStringColor = if (stringLineColor == Color.Unspecified) onSurface else stringLineColor
    val effectiveOpenColor   = if (openColor == Color.Unspecified) onSurface else openColor

    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .aspectRatio(0.7f)
            .padding(4.dp)
    ) {
        val topPad = size.height * 0.12f
        val bottomPad = size.height * 0.04f
        val leftPad = size.width * 0.14f
        val rightPad = size.width * 0.06f
        val baseFret = fingering.baseFret
        val fretLabelExtra = if (baseFret > 1) size.width * 0.06f else 0f
        val effectiveLeftPad = leftPad + fretLabelExtra

        val diagramWidth = size.width - effectiveLeftPad - rightPad
        val diagramHeight = size.height - topPad - bottomPad

        // Use actual position count for spacing calculation
        val actualStringCount = fingering.positions.size
        val stringSpacing = diagramWidth / (if (actualStringCount > 1) actualStringCount - 1 else 1)
        val fretSpacing = diagramHeight / displayedFrets

        // Draw nut or fret number
        if (baseFret == 1) {
            drawRect(
                color = nutColor,
                topLeft = Offset(effectiveLeftPad, topPad - fretSpacing * 0.12f),
                size = Size(diagramWidth, fretSpacing * 0.12f)
            )
        } else {
            val labelText = "$baseFret"
            val labelStyle = TextStyle(color = onSurface, fontSize = (size.height * 0.07f / density).sp)
            val measured = textMeasurer.measure(labelText, style = labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                topLeft = Offset(
                    x = effectiveLeftPad - size.width * 0.12f,
                    y = topPad - measured.size.height / 2f
                ),
                style = labelStyle
            )
        }

        // Draw fret lines
        for (f in 0..displayedFrets) {
            val y = topPad + f * fretSpacing
            drawLine(
                color = onSurface.copy(alpha = 0.4f),
                start = Offset(effectiveLeftPad, y),
                end = Offset(effectiveLeftPad + diagramWidth, y),
                strokeWidth = if (f == 0 && baseFret == 1) 0f else 1.5f
            )
        }

        // Draw string lines
        for (s in 0 until stringCount) {
            val x = effectiveLeftPad + s * stringSpacing
            drawLine(
                color = effectiveStringColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + diagramHeight),
                strokeWidth = 1.5f
            )
        }

        // Draw open/muted symbols above nut
        val symbolY = topPad - fretSpacing * 0.45f
        val symbolRadius = size.width * 0.035f
        fingering.positions.forEach { pos ->
            val x = effectiveLeftPad + pos.stringIndex * stringSpacing
            when {
                pos.fret == -1 -> {
                    // Muted X
                    drawLine(mutedColor, Offset(x - symbolRadius, symbolY - symbolRadius),
                        Offset(x + symbolRadius, symbolY + symbolRadius), 2f)
                    drawLine(mutedColor, Offset(x + symbolRadius, symbolY - symbolRadius),
                        Offset(x - symbolRadius, symbolY + symbolRadius), 2f)
                }
                pos.fret == 0 -> {
                    // Open circle
                    drawCircle(effectiveOpenColor, symbolRadius, Offset(x, symbolY), style =
                        androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }
        }

        // Draw barre
        fingering.barre?.let { barre ->
            val y = topPad + (barre.fret - baseFret + 0.5f) * fretSpacing
            val x1 = effectiveLeftPad + barre.fromString * stringSpacing
            val x2 = effectiveLeftPad + barre.toString * stringSpacing
            val barreRadius = fretSpacing * 0.35f
            drawRoundRect(
                color = barreColor,
                topLeft = Offset(x1, y - barreRadius),
                size = Size(x2 - x1, barreRadius * 2),
                cornerRadius = CornerRadius(barreRadius, barreRadius)
            )
        }

        // Draw finger dots
        fingering.positions
            .filter { it.fret > 0 }
            .forEach { pos ->
                val x = effectiveLeftPad + pos.stringIndex * stringSpacing
                val y = topPad + (pos.fret - baseFret + 0.5f) * fretSpacing
                drawCircle(dotColor, fretSpacing * 0.35f, Offset(x, y))
            }
    }
}
