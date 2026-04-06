package com.chordquiz.app.ui.components.chord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import com.chordquiz.app.data.model.BarreSegment
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.ui.theme.BarreColor
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.IncorrectRed
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.StringColor

/**
 * Tappable chord diagram.
 * Tap a fret position to toggle a finger dot on/off.
 * Tap above the nut to cycle a string: open → muted → open.
 * Drag right-to-left along a fret to draw a barre across multiple strings.
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
    totalFrets: Int = 21,
    initialFingering: Fingering? = null,
    incorrectFrettedStrings: Set<Int> = emptySet(),
    incorrectMutedStrings: Set<Int> = emptySet(),
    missedMuteStrings: Set<Int> = emptySet(),
    onFingeringChanged: (Fingering) -> Unit,
    onNoteSelected: ((stringIndex: Int, fret: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var effectiveBaseFret by remember {
        mutableStateOf(initialFingering?.baseFret ?: baseFret)
    }
    val initialPositions = initialFingering?.positions
        ?: (0 until stringCount).map { StringPosition(it, 0) }

    var positions by remember(stringCount) {
        mutableStateOf(initialPositions.toMutableList())
    }
    var barre by remember(stringCount) {
        mutableStateOf(initialFingering?.barre)
    }

    val textMeasurer = rememberTextMeasurer()

    var topPad = 0f
    var leftPad = 0f
    var effectiveLeftPad = 0f
    var stringSpacing = 0f
    var fretSpacing = 0f
    var diagramHeight = 0f

    Canvas(
        modifier = modifier
            .aspectRatio(0.7f)
            .padding(4.dp)
            .pointerInput(stringCount, displayedFrets) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (stringSpacing == 0f) return@awaitEachGesture

                    val startX = down.position.x
                    val startY = down.position.y

                    val rawString = ((startX - effectiveLeftPad) / stringSpacing).toInt()
                    val touchDownString = rawString.coerceIn(0, stringCount - 1)
                    val touchDownInFretGrid = startY >= topPad
                    val rawFret = ((startY - topPad) / fretSpacing).toInt() + effectiveBaseFret
                    val touchDownFret = rawFret.coerceIn(effectiveBaseFret, effectiveBaseFret + displayedFrets - 1)

                    // Snapshot base positions before any barre drag:
                    // strings previously absorbed by a barre are reset to open so
                    // a new drag (or a short drag falling back to tap) starts clean.
                    val existingBarre = barre
                    val basePositions: List<StringPosition> = if (existingBarre != null) {
                        positions.map { pos ->
                            if (pos.stringIndex in existingBarre.fromString..existingBarre.toString)
                                StringPosition(pos.stringIndex, 0)
                            else pos
                        }
                    } else positions.toList()

                    // Tap handler — defined here so it closes over all gesture-local variables
                    // and can be called from both the "pure tap" and "short drag" paths.
                    fun performTap() {
                        if (!touchDownInFretGrid) {
                            // Above nut: toggle open ↔ muted
                            val cur = positions.firstOrNull { it.stringIndex == touchDownString }?.fret ?: 0
                            val newFret = if (cur == -1) 0 else -1
                            positions = positions.toMutableList().also { list ->
                                val idx = list.indexOfFirst { it.stringIndex == touchDownString }
                                if (idx >= 0) list[idx] = StringPosition(touchDownString, newFret)
                            }
                            onFingeringChanged(Fingering(positions.toList(), barre, effectiveBaseFret))
                            if (newFret == 0) onNoteSelected?.invoke(touchDownString, 0)
                            return
                        }

                        val currentBarre = barre
                        if (currentBarre != null &&
                            touchDownString in currentBarre.fromString..currentBarre.toString &&
                            touchDownFret == currentBarre.fret
                        ) {
                            // Tap lands on an existing barre
                            val isEndpoint = touchDownString == currentBarre.fromString ||
                                touchDownString == currentBarre.toString
                            if (isEndpoint) {
                                // Shrink the barre span by detaching this endpoint string
                                val newFrom = if (touchDownString == currentBarre.fromString)
                                    currentBarre.fromString + 1 else currentBarre.fromString
                                val newTo = if (touchDownString == currentBarre.toString)
                                    currentBarre.toString - 1 else currentBarre.toString
                                barre = if (newTo - newFrom >= 1)
                                    BarreSegment(currentBarre.fret, newFrom, newTo)
                                else null
                                positions = positions.toMutableList().also { list ->
                                    val idx = list.indexOfFirst { it.stringIndex == touchDownString }
                                    if (idx >= 0) list[idx] = StringPosition(touchDownString, 0)
                                }
                            } else {
                                // Middle of barre: remove the entire barre and its dots
                                barre = null
                                positions = positions.toMutableList().also { list ->
                                    for (s in currentBarre.fromString..currentBarre.toString) {
                                        val idx = list.indexOfFirst { it.stringIndex == s }
                                        if (idx >= 0) list[idx] = StringPosition(s, 0)
                                    }
                                }
                            }
                            onFingeringChanged(Fingering(positions.toList(), barre, effectiveBaseFret))
                            return
                        }

                        // Regular fret tap: toggle dot on/off
                        val curPos = positions.firstOrNull { it.stringIndex == touchDownString }
                        val newFret = if (curPos?.fret == touchDownFret) 0 else touchDownFret
                        positions = positions.toMutableList().also { list ->
                            val idx = list.indexOfFirst { it.stringIndex == touchDownString }
                            if (idx >= 0) list[idx] = StringPosition(touchDownString, newFret)
                            else list.add(StringPosition(touchDownString, newFret))
                        }
                        onFingeringChanged(Fingering(positions.toList(), barre, effectiveBaseFret))
                        if (newFret > 0) onNoteSelected?.invoke(touchDownString, newFret)
                    }

                    var gestureClassified = false
                    var isBarreDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                        val dx = pointer.position.x - startX
                        val dy = pointer.position.y - startY

                        // Classify once horizontal movement reaches one full string spacing
                        if (!gestureClassified &&
                            kotlin.math.abs(dx) >= stringSpacing &&
                            kotlin.math.abs(dx) > kotlin.math.abs(dy)
                        ) {
                            gestureClassified = true
                            if (dx < 0 && touchDownInFretGrid) {
                                // Right-to-left drag in fret grid → barre mode
                                isBarreDrag = true
                                barre = null
                                positions = basePositions.toMutableList()
                            } else {
                                // Left-to-right drag (or above-nut drag) → let bubble to parent
                                break
                            }
                        }

                        // Classify once vertical movement reaches one full fret spacing
                        if (!gestureClassified &&
                            kotlin.math.abs(dy) >= fretSpacing &&
                            kotlin.math.abs(dy) > kotlin.math.abs(dx)
                        ) {
                            gestureClassified = true
                            val delta = if (dy < 0) 1 else -1  // swipe up → higher up neck
                            val maxBaseFret = (totalFrets - displayedFrets + 1).coerceAtLeast(1)
                            val newBase = (effectiveBaseFret + delta).coerceIn(1, maxBaseFret)
                            if (newBase != effectiveBaseFret) {
                                effectiveBaseFret = newBase
                                onFingeringChanged(Fingering(positions.toList(), barre, newBase))
                            }
                            break
                        }

                        if (isBarreDrag) {
                            pointer.consume()
                            val rawCurrent = ((pointer.position.x - effectiveLeftPad) / stringSpacing).toInt()
                            // Rubber-band: span from leftmost reached back to touchDownString
                            val barreEndString = minOf(
                                rawCurrent.coerceIn(0, stringCount - 1),
                                touchDownString
                            )

                            if (barreEndString < touchDownString) {
                                val span = barreEndString..touchDownString
                                // Recompute from base each frame so rubber-band reversal is clean
                                val newPositions = basePositions.map { pos ->
                                    if (pos.stringIndex in span && pos.fret <= touchDownFret)
                                        StringPosition(pos.stringIndex, touchDownFret)
                                    else pos
                                }
                                // Actual barre span = only strings that were absorbed (fret ≤ barreFret)
                                val absorbed = span.filter { s ->
                                    (basePositions.firstOrNull { it.stringIndex == s }?.fret ?: 0) <= touchDownFret
                                }
                                barre = if (absorbed.size >= 2)
                                    BarreSegment(touchDownFret, absorbed.min(), absorbed.max())
                                else null
                                positions = newPositions.toMutableList()
                            } else {
                                // Dragged back to starting string
                                barre = null
                                positions = basePositions.toMutableList()
                            }
                        }

                        if (!pointer.pressed) {
                            val committedBarre = barre
                            when {
                                isBarreDrag && committedBarre != null -> {
                                    // Commit the barre
                                    onFingeringChanged(Fingering(positions.toList(), committedBarre, effectiveBaseFret))
                                }
                                isBarreDrag -> {
                                    // Drag didn't reach 2+ strings → restore and treat as tap
                                    positions = basePositions.toMutableList()
                                    barre = existingBarre
                                    performTap()
                                }
                                !gestureClassified -> {
                                    // Pure tap
                                    performTap()
                                }
                            }
                            break
                        }
                    }
                }
            }
    ) {
        topPad = size.height * 0.12f
        val bottomPad = size.height * 0.04f
        leftPad = size.width * 0.14f
        val rightPad = size.width * 0.06f
        val fretLabelExtra = if (effectiveBaseFret > 1) size.width * 0.06f else 0f
        effectiveLeftPad = leftPad + fretLabelExtra

        val diagramWidth = size.width - effectiveLeftPad - rightPad
        diagramHeight = size.height - topPad - bottomPad
        stringSpacing = diagramWidth / (stringCount - 1)
        fretSpacing = diagramHeight / displayedFrets

        // Nut / fret number
        if (effectiveBaseFret == 1) {
            drawRect(
                color = NutBrown,
                topLeft = Offset(effectiveLeftPad, topPad - fretSpacing * 0.12f),
                size = Size(diagramWidth, fretSpacing * 0.12f)
            )
        } else {
            val labelText = "$effectiveBaseFret"
            val labelStyle = TextStyle(color = Color.Black, fontSize = (size.height * 0.07f / density).sp)
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

        // Fret lines
        for (f in 0..displayedFrets) {
            val y = topPad + f * fretSpacing
            drawLine(Color.Gray, Offset(effectiveLeftPad, y), Offset(effectiveLeftPad + diagramWidth, y), 1.5f)
        }

        // String lines
        for (s in 0 until stringCount) {
            val x = effectiveLeftPad + s * stringSpacing
            drawLine(StringColor, Offset(x, topPad), Offset(x, topPad + diagramHeight), 1.5f)
        }

        // Above-nut markers (open circle or muted X)
        val symbolY = topPad - fretSpacing * 0.45f
        val symbolRadius = size.width * 0.035f
        positions.forEach { pos ->
            val x = effectiveLeftPad + pos.stringIndex * stringSpacing
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
                    // Open circle — red if this string should have been muted or fretted, black otherwise
                    val oColor = if (pos.stringIndex in missedMuteStrings || pos.stringIndex in incorrectFrettedStrings) IncorrectRed else Color.Black
                    drawCircle(oColor, symbolRadius, Offset(x, symbolY), style = Stroke(2f))
                }
            }
        }

        val visibleRange = effectiveBaseFret..(effectiveBaseFret + displayedFrets - 1)

        // Barre (drawn before finger dots so dots render on top)
        barre?.takeIf { it.fret in visibleRange }?.let { b ->
            val y = topPad + (b.fret - effectiveBaseFret + 0.5f) * fretSpacing
            val x1 = effectiveLeftPad + b.fromString * stringSpacing
            val x2 = effectiveLeftPad + b.toString * stringSpacing
            val barreRadius = fretSpacing * 0.35f
            drawRoundRect(
                color = BarreColor,
                topLeft = Offset(x1, y - barreRadius),
                size = Size(x2 - x1, barreRadius * 2),
                cornerRadius = CornerRadius(barreRadius, barreRadius)
            )
        }

        // Finger dots
        positions.filter { it.fret > 0 && it.fret in visibleRange }.forEach { pos ->
            val x = effectiveLeftPad + pos.stringIndex * stringSpacing
            val y = topPad + (pos.fret - effectiveBaseFret + 0.5f) * fretSpacing
            val dotColor = if (pos.stringIndex in incorrectFrettedStrings) IncorrectRed else FingerDot
            drawCircle(dotColor, fretSpacing * 0.35f, Offset(x, y))
        }

        // Faint tap-target hint grid
        for (s in 0 until stringCount) {
            for (f in 0 until displayedFrets) {
                val cx = effectiveLeftPad + s * stringSpacing
                val cy = topPad + (f + 0.5f) * fretSpacing
                drawCircle(Color.Gray.copy(alpha = 0.15f), fretSpacing * 0.3f, Offset(cx, cy))
            }
        }
    }
}
