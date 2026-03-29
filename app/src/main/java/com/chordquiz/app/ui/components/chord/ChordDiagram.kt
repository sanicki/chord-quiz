package com.chordquiz.app.ui.components.chord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.ui.theme.BarreColor
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.StringColor

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
        stringCount = chord.fingerings.first().positions.size,
        modifier = modifier
    )
}

/**
 * Core Canvas-based chord diagram renderer.
 * Can be used for both static display and interactive overlays.
 */
@Composable
fun ChordDiagramCanvas(
    fingering: Fingering,
    stringCount: Int,
    modifier: Modifier = Modifier,
    displayedFrets: Int = 5,
    dotColor: Color = FingerDot,
    stringLineColor: Color = StringColor,
    nutColor: Color = NutBrown,
    barreColor: Color = BarreColor,
    mutedColor: Color = MutedGray,
    openColor: Color = Color.Black
) {
    Canvas(
        modifier = modifier
            .aspectRatio(0.7f)
            .padding(4.dp)
    ) {
        val topPad = size.height * 0.12f
        val bottomPad = size.height * 0.04f
        val leftPad = size.width * 0.14f
        val rightPad = size.width * 0.06f

        val diagramWidth = size.width - leftPad - rightPad
        val diagramHeight = size.height - topPad - bottomPad

        val stringSpacing = diagramWidth / (stringCount - 1)
        val fretSpacing = diagramHeight / displayedFrets

        val baseFret = fingering.baseFret

        // Draw nut or fret number
        if (baseFret == 1) {
            drawRect(
                color = nutColor,
                topLeft = Offset(leftPad, topPad - fretSpacing * 0.12f),
                size = Size(diagramWidth, fretSpacing * 0.12f)
            )
        } else {
            drawContext.canvas.nativeCanvas.drawText(
                "${baseFret}fr",
                leftPad - size.width * 0.12f,
                topPad + fretSpacing * 0.6f,
                android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = size.height * 0.07f
                    isAntiAlias = true
                }
            )
        }

        // Draw fret lines
        for (f in 0..displayedFrets) {
            val y = topPad + f * fretSpacing
            drawLine(
                color = Color.Gray,
                start = Offset(leftPad, y),
                end = Offset(leftPad + diagramWidth, y),
                strokeWidth = if (f == 0 && baseFret == 1) 0f else 1.5f
            )
        }

        // Draw string lines
        for (s in 0 until stringCount) {
            val x = leftPad + s * stringSpacing
            drawLine(
                color = stringLineColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + diagramHeight),
                strokeWidth = 1.5f
            )
        }

        // Draw open/muted symbols above nut
        val symbolY = topPad - fretSpacing * 0.45f
        val symbolRadius = size.width * 0.035f
        fingering.positions.forEach { pos ->
            val x = leftPad + pos.stringIndex * stringSpacing
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
                    drawCircle(openColor, symbolRadius, Offset(x, symbolY), style =
                        androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }
        }

        // Draw barre
        fingering.barre?.let { barre ->
            val y = topPad + (barre.fret - baseFret + 0.5f) * fretSpacing
            val x1 = leftPad + barre.fromString * stringSpacing
            val x2 = leftPad + barre.toString * stringSpacing
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
                val x = leftPad + pos.stringIndex * stringSpacing
                val y = topPad + (pos.fret - baseFret + 0.5f) * fretSpacing
                drawCircle(dotColor, fretSpacing * 0.35f, Offset(x, y))
            }
    }
}
