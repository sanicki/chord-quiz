package com.chordquiz.app.ui.components.chord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.ui.theme.BarreColor
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.StringColor

/**
 * Tappable chord diagram.
 * Tap a fret position to cycle: empty → finger dot → muted → empty.
 * Tap above nut to cycle string: empty → open → muted → empty.
 *
 * @param stringCount number of strings for this instrument
 * @param onFingeringChanged called each time a position changes
 */
@Composable
fun InteractiveChordDiagram(
    stringCount: Int,
    displayedFrets: Int = 5,
    baseFret: Int = 1,
    initialFingering: Fingering? = null,
    onFingeringChanged: (Fingering) -> Unit,
    modifier: Modifier = Modifier
) {
    // positions indexed by stringIndex
    val initialPositions = initialFingering?.positions
        ?: (0 until stringCount).map { StringPosition(it, 0) }

    var positions by remember(stringCount) {
        mutableStateOf(initialPositions.toMutableList())
    }

    val textMeasurer = rememberTextMeasurer()

    // Expose layout metrics so tap detector can convert coordinates
    var topPad = 0f
    var leftPad = 0f
    var stringSpacing = 0f
    var fretSpacing = 0f
    var diagramHeight = 0f

    Canvas(
        modifier = modifier
            .aspectRatio(0.7f)
            .padding(4.dp)
            .pointerInput(stringCount, displayedFrets) {
                detectTapGestures { offset ->
                    if (stringSpacing == 0f) return@detectTapGestures

                    // Determine tapped string
                    val rawString = ((offset.x - leftPad) / stringSpacing).toInt()
                    val tappedString = rawString.coerceIn(0, stringCount - 1)

                    // Tap above nut → toggle open/muted
                    if (offset.y < topPad) {
                        val cur = positions.firstOrNull { it.stringIndex == tappedString }?.fret ?: 0
                        val newFret = when (cur) {
                            0 -> -1   // open → muted
                            -1 -> 0  // muted → open
                            else -> 0
                        }
                        positions = positions.toMutableList().also { list ->
                            val idx = list.indexOfFirst { it.stringIndex == tappedString }
                            if (idx >= 0) list[idx] = StringPosition(tappedString, newFret)
                        }
                        onFingeringChanged(Fingering(positions.toList(), baseFret = baseFret))
                        return@detectTapGestures
                    }

                    // Tap on fret grid
                    val rawFret = ((offset.y - topPad) / fretSpacing).toInt() + baseFret
                    val tappedFret = rawFret.coerceIn(baseFret, baseFret + displayedFrets - 1)

                    val curPos = positions.firstOrNull { it.stringIndex == tappedString }
                    val newFret = if (curPos?.fret == tappedFret) 0 else tappedFret

                    positions = positions.toMutableList().also { list ->
                        val idx = list.indexOfFirst { it.stringIndex == tappedString }
                        if (idx >= 0) list[idx] = StringPosition(tappedString, newFret)
                        else list.add(StringPosition(tappedString, newFret))
                    }
                    onFingeringChanged(Fingering(positions.toList(), baseFret = baseFret))
                }
            }
    ) {
        topPad = size.height * 0.12f
        val bottomPad = size.height * 0.04f
        leftPad = size.width * 0.14f
        val rightPad = size.width * 0.06f

        val diagramWidth = size.width - leftPad - rightPad
        diagramHeight = size.height - topPad - bottomPad
        stringSpacing = diagramWidth / (stringCount - 1)
        fretSpacing = diagramHeight / displayedFrets

        // Nut
        if (baseFret == 1) {
            drawRect(
                color = NutBrown,
                topLeft = Offset(leftPad, topPad - fretSpacing * 0.12f),
                size = Size(diagramWidth, fretSpacing * 0.12f)
            )
        } else {
            drawText(
                textMeasurer = textMeasurer,
                text = "${baseFret}fr",
                topLeft = Offset(
                    leftPad - size.width * 0.12f,
                    topPad + fretSpacing * 0.3f
                ),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = (size.height * 0.07f / density).sp
                )
            )
        }

        // Fret lines
        for (f in 0..displayedFrets) {
            val y = topPad + f * fretSpacing
            drawLine(Color.Gray, Offset(leftPad, y), Offset(leftPad + diagramWidth, y), 1.5f)
        }

        // String lines
        for (s in 0 until stringCount) {
            val x = leftPad + s * stringSpacing
            drawLine(StringColor, Offset(x, topPad), Offset(x, topPad + diagramHeight), 1.5f)
        }

        // Open/muted markers
        val symbolY = topPad - fretSpacing * 0.45f
        val symbolRadius = size.width * 0.035f
        positions.forEach { pos ->
            val x = leftPad + pos.stringIndex * stringSpacing
            when (pos.fret) {
                -1 -> {
                    drawLine(MutedGray, Offset(x - symbolRadius, symbolY - symbolRadius),
                        Offset(x + symbolRadius, symbolY + symbolRadius), 2f)
                    drawLine(MutedGray, Offset(x + symbolRadius, symbolY - symbolRadius),
                        Offset(x - symbolRadius, symbolY + symbolRadius), 2f)
                }
                0 -> drawCircle(Color.Black, symbolRadius, Offset(x, symbolY), style = Stroke(2f))
            }
        }

        // Finger dots
        positions.filter { it.fret > 0 }.forEach { pos ->
            val x = leftPad + pos.stringIndex * stringSpacing
            val y = topPad + (pos.fret - baseFret + 0.5f) * fretSpacing
            drawCircle(FingerDot, fretSpacing * 0.35f, Offset(x, y))
        }

        // Tap-target hint grid (faint cells)
        for (s in 0 until stringCount) {
            for (f in 0 until displayedFrets) {
                val cx = leftPad + s * stringSpacing
                val cy = topPad + (f + 0.5f) * fretSpacing
                drawCircle(Color.Gray.copy(alpha = 0.15f), fretSpacing * 0.3f, Offset(cx, cy))
            }
        }
    }
}
