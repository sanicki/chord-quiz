package com.chordquiz.app.ui.components.staff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

data class StaffNote(val semitone: Int, val octave: Int, val displayName: String)

enum class Clef { TREBLE, BASS }

/**
 * Canvas-based music staff displaying quarter notes for missed notes in ascending pitch order.
 *
 * Staff lines are counted from bottom (line 1) to top (line 5) in standard notation convention.
 * Each staff "step" is half a line spacing (either on a line or in a space).
 */
@Composable
fun MusicStaff(
    notes: List<StaffNote>,
    clef: Clef,
    modifier: Modifier = Modifier
) {
    val noteWidth = 60.dp
    val clefWidth = 48.dp
    val totalWidth = clefWidth + noteWidth * notes.size.coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .width(totalWidth)
    ) {
        val lineSpacing = size.height / 8f
        val staffTop = lineSpacing * 1.5f
        val staffBottom = staffTop + lineSpacing * 4f

        val inkColor = MaterialTheme.colorScheme.onSurface

        // Draw 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(inkColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }

        // Draw clef symbol using Unicode music characters
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = inkColor.toArgb()
                textSize = lineSpacing * 2.5f
                isAntiAlias = true
            }
            val clefSymbol = if (clef == Clef.TREBLE) "𝄞" else "𝄢"
            canvas.nativeCanvas.drawText(
                clefSymbol,
                4f,
                staffBottom + lineSpacing * 0.3f,
                paint
            )
        }

        val clefPx = clefWidth.toPx()
        val noteWidthPx = noteWidth.toPx()

        // Draw each note
        notes.forEachIndexed { index, staffNote ->
            val cx = clefPx + noteWidthPx * index + noteWidthPx / 2f
            val step = noteToStaffStep(staffNote.semitone, staffNote.octave, clef)
            // step 0 = middle C (C4), step is half line-spacings from middle C reference
            val middleCY = middleCYPosition(staffTop, staffBottom, lineSpacing, clef)
            val noteY = middleCY - step * (lineSpacing / 2f)

            val noteHeadRx = lineSpacing * 0.45f
            val noteHeadRy = lineSpacing * 0.35f

            // Draw ledger lines if needed
            drawLedgerLines(cx, noteY, staffTop, staffBottom, lineSpacing, noteHeadRx, inkColor)

            // Draw note head (filled oval)
            drawOval(
                color = inkColor,
                topLeft = Offset(cx - noteHeadRx, noteY - noteHeadRy),
                size = androidx.compose.ui.geometry.Size(noteHeadRx * 2f, noteHeadRy * 2f)
            )

            // Draw stem (up if below middle line, down if above)
            val middleLineY = staffTop + lineSpacing * 2f
            val stemUp = noteY > middleLineY
            val stemStartY = if (stemUp) noteY - noteHeadRy else noteY + noteHeadRy
            val stemEndY = if (stemUp) noteY - lineSpacing * 3.5f else noteY + lineSpacing * 3.5f
            val stemX = if (stemUp) cx + noteHeadRx else cx - noteHeadRx
            drawLine(inkColor, Offset(stemX, stemStartY), Offset(stemX, stemEndY), strokeWidth = 2f)

            // Draw accidental
            val accidental = when {
                staffNote.displayName.endsWith("#") -> "♯"
                staffNote.displayName.endsWith("b") -> "♭"
                else -> null
            }
            if (accidental != null) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = inkColor.toArgb()
                        textSize = lineSpacing * 1.1f
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        accidental,
                        cx - noteHeadRx - lineSpacing * 1.1f,
                        noteY + lineSpacing * 0.35f,
                        paint
                    )
                }
            }

            // Draw note name label below staff
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = inkColor.toArgb()
                    textSize = lineSpacing * 0.8f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    staffNote.displayName,
                    cx,
                    staffBottom + lineSpacing * 1.5f,
                    paint
                )
            }
        }
    }
}

/**
 * Returns the staff step for a note, where step=0 is middle C (C4).
 * Each step is one half-line-space (either line or space).
 *
 * For treble clef, lines (bottom to top) are E4, G4, B4, D5, F5
 * For bass clef, lines (bottom to top) are G2, B2, D3, F3, A3
 */
private fun noteToStaffStep(semitone: Int, octave: Int, clef: Clef): Int {
    // Pitch class to diatonic step within octave (0=C, 1=D, 2=E, 3=F, 4=G, 5=A, 6=B)
    val diatonicStep = semitoneToDiatonic(semitone)
    // Absolute diatonic step from C0
    val absoluteStep = octave * 7 + diatonicStep
    // Middle C (C4) is at absolute step = 4*7 + 0 = 28
    return absoluteStep - 28
}

/**
 * Converts semitone (0=C) to diatonic step (0=C, 1=D, 2=E, 3=F, 4=G, 5=A, 6=B).
 * For accidentals (sharps/flats), rounds to nearest diatonic note.
 */
private fun semitoneToDiatonic(semitone: Int): Int {
    return when (semitone % 12) {
        0 -> 0   // C
        1 -> 0   // C#/Db → use C position (accidental drawn separately)
        2 -> 1   // D
        3 -> 2   // D#/Eb → use E position
        4 -> 2   // E
        5 -> 3   // F
        6 -> 3   // F#/Gb → use F position
        7 -> 4   // G
        8 -> 4   // G#/Ab → use G position
        9 -> 5   // A
        10 -> 5  // A#/Bb → use A position
        11 -> 6  // B
        else -> 0
    }
}

/**
 * Returns the Y position of middle C (C4) on the staff.
 *
 * Treble clef: middle C is one ledger line below the staff (step -2 from E4 bottom line).
 * Bass clef: middle C is one ledger line above the staff (step +2 from A3 top line).
 */
private fun middleCYPosition(staffTop: Float, staffBottom: Float, lineSpacing: Float, clef: Clef): Float {
    return when (clef) {
        Clef.TREBLE -> staffBottom + lineSpacing  // one ledger line below bottom (E4) line
        Clef.BASS -> staffTop - lineSpacing        // one ledger line above top (A3) line
    }
}

private fun DrawScope.drawLedgerLines(
    cx: Float,
    noteY: Float,
    staffTop: Float,
    staffBottom: Float,
    lineSpacing: Float,
    noteHeadRx: Float,
    color: Color
) {
    val ledgerHalfWidth = noteHeadRx * 1.4f
    val tolerance = lineSpacing * 0.1f

    // Ledger lines below staff
    var y = staffBottom + lineSpacing
    while (y <= noteY + tolerance) {
        drawLine(color, Offset(cx - ledgerHalfWidth, y), Offset(cx + ledgerHalfWidth, y), strokeWidth = 2f)
        y += lineSpacing
    }

    // Ledger lines above staff
    y = staffTop - lineSpacing
    while (y >= noteY - tolerance) {
        drawLine(color, Offset(cx - ledgerHalfWidth, y), Offset(cx + ledgerHalfWidth, y), strokeWidth = 2f)
        y -= lineSpacing
    }
}
