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
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.IncorrectRed
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.StringColor

/**
 * Tappable chord diagram.
 * Tap a fret position to toggle a finger dot on/off.
 * Tap above the nut to cycle a string: open → muted → open.
 *
 * @param incorrectFrettedStrings  strings where the user placed a wrong-note finger (shown red)
 * @param incorrectMutedStrings    strings the user muted but shouldn't have (X shown red)
 * @param missedMuteStrings        strings that should be muted but the user left open/fretted
 *                                  (open-circle shown red as a hint)
 * @param onNoteSelected           called when a finger is placed (fret > 0) or a string opened (fret == 0)
 */
@Composable
fun InteractiveChordDiagram(
    stringCount: Int,
    displayedFrets: Int = 5,
    baseFret: Int = 1,
    initialFingering: Fingering? = null,
    incorrectFrettedStrings: Set<Int> = emptySet(),
    incorrectMutedStrings: Set<Int> = emptySet(),
    missedMuteStrings: Set<Int> = emptySet(),
    onFingeringChanged: (Fingering) -> Unit,
    onNoteSelected: ((stringIndex: Int, fret: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val initialPositions = initialFingering?.positions
        ?: (0 until stringCount).map { StringPosition(it, 0) }

    var positions by remember(stringCount) {
        mutableStateOf(initialPositions.toMutableList())
    }

    val textMeasurer = rememberTextMeasurer()

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

                    val rawString = ((offset.x - leftPad) / stringSpacing).toInt()
                    val tappedString = rawString.coerceIn(0, stringCount - 1)

                    if (offset.y < topPad) {
                        // Above nut: toggle open ↔ muted
                        val cur = positions.firstOrNull { it.stringIndex == tappedString }?.fret ?: 0
                        val newFret = if (cur == -1) 0 else -1
                        positions = positions.toMutableList().also { list ->
                            val idx = list.indexOfFirst { it.stringIndex == tappedString }
                            if (idx >= 0) list[idx] = StringPosition(tappedString, newFret)
                        }
                        onFingeringChanged(Fingering(positions.toList(), baseFret = baseFret))
                        if (newFret == 0) onNoteSelected?.invoke(tappedString, 0)
                        return@detectTapGestures
                    }

                    // Fret grid tap
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
                    if (newFret > 0) onNoteSelected?.invoke(tappedString, newFret)
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

        // Nut / fret number
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
                topLeft = Offset(leftPad - size.width * 0.12f, topPad + fretSpacing * 0.3f),
                style = TextStyle(color = Color.Black, fontSize = (size.height * 0.07f / density).sp)
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

        // Above-nut markers (open circle or muted X)
        val symbolY = topPad - fretSpacing * 0.45f
        val symbolRadius = size.width * 0.035f
        positions.forEach { pos ->
            val x = leftPad + pos.stringIndex * stringSpacing
            when (pos.fret) {
                -1 -> {
                    // Muted X — red if incorrectly muted, gray otherwise
                    val xColor = if (pos.stringIndex in incorrectMutedStrings) IncorrectRed else MutedGray
                    drawLine(xColor, Offset(x - symbolRadius, symbolY - symbolRadius),
                        Offset(x + symbolRadius, symbolY + symbolRadius), 2f)
                    drawLine(xColor, Offset(x + symbolRadius, symbolY - symbolRadius),
                        Offset(x - symbolRadius, symbolY + symbolRadius), 2f)
                }
                0 -> {
                    // Open circle — red if this string should have been muted, black otherwise
                    val oColor = if (pos.stringIndex in missedMuteStrings) IncorrectRed else Color.Black
                    drawCircle(oColor, symbolRadius, Offset(x, symbolY), style = Stroke(2f))
                }
            }
        }

        // Finger dots
        positions.filter { it.fret > 0 }.forEach { pos ->
            val x = leftPad + pos.stringIndex * stringSpacing
            val y = topPad + (pos.fret - baseFret + 0.5f) * fretSpacing
            val dotColor = if (pos.stringIndex in incorrectFrettedStrings) IncorrectRed else FingerDot
            drawCircle(dotColor, fretSpacing * 0.35f, Offset(x, y))
        }

        // Faint tap-target hint grid
        for (s in 0 until stringCount) {
            for (f in 0 until displayedFrets) {
                val cx = leftPad + s * stringSpacing
                val cy = topPad + (f + 0.5f) * fretSpacing
                drawCircle(Color.Gray.copy(alpha = 0.15f), fretSpacing * 0.3f, Offset(cx, cy))
            }
        }
    }
}
