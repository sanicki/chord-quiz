package com.chordquiz.app.ui.components.staff

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.chordquiz.app.audio.NotePlayer
import com.chordquiz.app.domain.semitoneToDiatonic
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Nine quarter-notes spanning from E4 (bottom treble line) to F5 (top treble line). */
private data class GradeNote(val midi: Int, val semitone: Int, val octave: Int)

private val GRADE_NOTES = listOf(
    GradeNote(midi = 64, semitone = 4,  octave = 4),  // E4 — line 1
    GradeNote(midi = 65, semitone = 5,  octave = 4),  // F4 — space 1
    GradeNote(midi = 67, semitone = 7,  octave = 4),  // G4 — line 2
    GradeNote(midi = 69, semitone = 9,  octave = 4),  // A4 — space 2
    GradeNote(midi = 71, semitone = 11, octave = 4),  // B4 — line 3 (middle)
    GradeNote(midi = 72, semitone = 0,  octave = 5),  // C5 — space 3
    GradeNote(midi = 74, semitone = 2,  octave = 5),  // D5 — line 4
    GradeNote(midi = 76, semitone = 4,  octave = 5),  // E5 — space 4
    GradeNote(midi = 77, semitone = 5,  octave = 5),  // F5 — line 5
)

/**
 * Static gradient: note 0 is always Red, note 4 always Yellow, note 8 always Green.
 * Intermediate colours are linearly interpolated.
 */
private val NOTE_COLORS: List<Color> = run {
    val red    = Color(0xFFE53935)
    val yellow = Color(0xFFFFD600)
    val green  = Color(0xFF43A047)
    (0..8).map { i ->
        if (i <= 4) lerp(red, yellow, i / 4f) else lerp(yellow, green, (i - 4) / 4f)
    }
}

/**
 * Returns the staff step for a grade note, where step 0 = middle C (C4).
 * Each step is one half-line-spacing (either a line or a space).
 * Middle C absolute step = 4 × 7 + 0 = 28.
 */
private fun gradeNoteToStaffStep(note: GradeNote): Int {
    val absoluteStep = note.octave * 7 + semitoneToDiatonic(note.semitone)
    return absoluteStep - 28
}

/**
 * A 5-line treble staff that grades quiz performance by filling 0–9 quarter-notes
 * left-to-right, using a Red → Yellow → Green gradient for filled notes.
 *
 * Notes are animated sequentially on first composition; each note plays its pitch
 * via [NotePlayer] as it fills.
 *
 * @param scorePercent  Integer percentage (0–100); 50 % → 5 notes, 100 % → 9 notes.
 * @param instrumentId  Instrument timbre for audio playback.
 * @param modifier      Applied to the underlying [Canvas].
 */
@Composable
fun MusicalGradeStaff(
    scorePercent: Int,
    instrumentId: String = "guitar_standard",
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
) {
    val targetFilled = (scorePercent * 9.0 / 100.0).roundToInt().coerceIn(0, 9)
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetFilled) {
        animatable.snapTo(0f)
        for (i in 0 until targetFilled) {
            // Fire audio concurrently with the visual animation for this note.
            launch { NotePlayer.playNote(GRADE_NOTES[i].midi, instrumentId, durationMs = 400) }
            animatable.animateTo(
                targetValue = (i + 1).toFloat(),
                animationSpec = tween(durationMillis = 350)
            )
        }
    }

    // Reading animatable.value here creates a state dependency: the composable
    // recomposes (and the Canvas redraws) on every animation frame.
    val progress = animatable.value

    val inkColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        // Discrete fill: notes 0..(filledCount-1) are shown in colour.
        val filledCount = progress.toInt()

        // ── Layout ─────────────────────────────────────────────────────────────
        val lineSpacing = size.height / 8f
        val staffTop    = lineSpacing * 1.5f
        val staffBottom = staffTop + lineSpacing * 4f
        val clefWidth   = lineSpacing * 3f
        val noteArea    = size.width - clefWidth
        val noteSpacing = noteArea / GRADE_NOTES.size.toFloat()

        // ── 5 staff lines ──────────────────────────────────────────────────────
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(inkColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }

        // ── Treble clef ────────────────────────────────────────────────────────
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color       = inkColor.toArgb()
                textSize    = lineSpacing * 2.5f
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawText("𝄞", 4f, staffBottom + lineSpacing * 0.3f, paint)
        }

        // ── Notes ──────────────────────────────────────────────────────────────
        // In treble clef, middle C (C4) is one ledger line below the bottom staff line (E4).
        val middleCY    = staffBottom + lineSpacing
        val middleLineY = staffTop + lineSpacing * 2f   // B4 — centre staff line

        GRADE_NOTES.forEachIndexed { index, note ->
            val cx    = clefWidth + noteSpacing * index + noteSpacing / 2f
            val step  = gradeNoteToStaffStep(note)
            val noteY = middleCY - step * (lineSpacing / 2f)

            val isFilled  = index < filledCount
            val noteColor = if (isFilled) NOTE_COLORS[index] else inkColor.copy(alpha = 0.2f)

            val rx = lineSpacing * 0.45f
            val ry = lineSpacing * 0.35f

            // Note head
            drawOval(
                color   = noteColor,
                topLeft = Offset(cx - rx, noteY - ry),
                size    = Size(rx * 2f, ry * 2f)
            )

            // Stem: up when the note is on or below the middle line, down when above.
            val stemUp     = noteY >= middleLineY
            val stemStartY = if (stemUp) noteY - ry else noteY + ry
            val stemEndY   = if (stemUp) noteY - lineSpacing * 3.5f else noteY + lineSpacing * 3.5f
            val stemX      = if (stemUp) cx + rx else cx - rx
            drawLine(noteColor, Offset(stemX, stemStartY), Offset(stemX, stemEndY), strokeWidth = 2f)
        }
    }
}
