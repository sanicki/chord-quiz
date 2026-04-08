package com.chordquiz.app.ui.components.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordquiz.app.audio.StringTuningState
import com.chordquiz.app.audio.TuningZone
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.IncorrectRed
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.SecondaryAmber

/**
 * Vertical fretboard view for the Tuner screen.
 *
 * Strings run top-to-bottom as vertical columns; frets are horizontal lines.
 * String index 0 = lowest/thickest on the left, index N-1 = highest on the right
 * (matching standard guitar tab orientation). Open string note names are drawn
 * above the nut. The active string glows in zone color:
 *   - Red  (>1 semitone off), Yellow (within 1 semitone), Green (in tune ±10¢).
 * Ambiguous strings glow yellow at 50% alpha.
 *
 * The background is transparent — the Tuner screen's own background shows through.
 * All structural strokes (fret lines, inactive strings) use
 * [MaterialTheme.colorScheme.onSurface] for light/dark-mode adaptability.
 */
@Composable
fun TunerFretboardView(
    instrument: Instrument,
    stringStates: List<StringTuningState>,
    modifier: Modifier = Modifier,
    fretboardHeight: Dp = 220.dp
) {
    val density = LocalDensity.current
    val labelTextSize = with(density) { 12.sp.toPx() }
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(fretboardHeight)
    ) {
        val stringCount = instrument.stringCount

        // Vertical padding so fret area doesn't touch top/bottom edges
        val labelHeight = labelTextSize * 2f
        val nutHeight   = 6f
        val topPad      = labelHeight + nutHeight + 4f
        val bottomPad   = 12f
        val fretAreaTop    = topPad
        val fretAreaBottom = size.height - bottomPad
        val fretAreaHeight = fretAreaBottom - fretAreaTop

        // Horizontal padding so strings don't touch the left/right edges
        val horizontalPad = size.width * 0.06f
        val usableWidth   = size.width - 2 * horizontalPad

        // String X positions — index 0 at left (lowest/thickest), index N-1 at right
        val stringXs = List(stringCount) { i ->
            horizontalPad + usableWidth * i / (stringCount - 1).coerceAtLeast(1)
        }

        // --- Fret lines (5 visible frets) ---
        val fretCount = 5
        for (f in 0..fretCount) {
            val y = fretAreaTop + fretAreaHeight * f / fretCount
            drawLine(
                color = onSurface.copy(alpha = if (f == 0) 0.3f else 0.4f),
                start = Offset(horizontalPad, y),
                end   = Offset(horizontalPad + usableWidth, y),
                strokeWidth = if (f == 0) 1f else 1.5f
            )
        }

        // --- Nut ---
        drawRect(
            color = NutBrown,
            topLeft = Offset(horizontalPad - 2f, fretAreaTop - nutHeight),
            size = Size(usableWidth + 4f, nutHeight)
        )

        // --- Strings and note labels ---
        for (i in 0 until stringCount) {
            val state = stringStates.getOrNull(i)
            val x = stringXs[i]

            // String thickness: thicker strings at lower indices (physical feel)
            val baseThickness = 2f + (stringCount - 1 - i) * 0.5f

            val (strokeColor, strokeWidth, alpha) = resolveStringStyle(
                state              = state,
                baseThickness      = baseThickness,
                defaultStringColor = onSurface
            )

            drawLine(
                color       = strokeColor.copy(alpha = alpha),
                start       = Offset(x, fretAreaTop),
                end         = Offset(x, fretAreaBottom),
                strokeWidth = strokeWidth,
                cap         = StrokeCap.Round
            )

            // Note label above the nut
            val label = state?.openNote?.displayName
                ?: instrument.openStringNotes.getOrNull(i)?.displayName
                ?: ""

            val labelColor = when {
                state?.isAmbiguous == true -> SecondaryAmber.copy(alpha = 0.7f)
                state?.tuningZone == TuningZone.IN_TUNE -> CorrectGreen
                state?.tuningZone in listOf(TuningZone.FLAT_RED, TuningZone.SHARP_RED) -> IncorrectRed
                state?.tuningZone in listOf(TuningZone.FLAT_YELLOW, TuningZone.SHARP_YELLOW) -> SecondaryAmber
                else -> onSurface.copy(alpha = 0.7f)
            }

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize    = labelTextSize
                    color       = labelColor.toArgb()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    typeface    = android.graphics.Typeface.DEFAULT_BOLD
                }
                // Draw label above the nut
                canvas.nativeCanvas.drawText(
                    label,
                    x,
                    fretAreaTop - nutHeight - 2f,
                    paint
                )
            }
        }
    }
}

/** Returns (color, strokeWidth, alpha) for a string based on its tuning state. */
private fun resolveStringStyle(
    state: StringTuningState?,
    baseThickness: Float,
    defaultStringColor: Color
): Triple<Color, Float, Float> {
    if (state == null) return Triple(defaultStringColor, baseThickness, 0.4f)

    return when {
        state.isAmbiguous ->
            Triple(SecondaryAmber, baseThickness + 2f, 0.5f)
        state.tuningZone == TuningZone.IN_TUNE ->
            Triple(CorrectGreen, baseThickness + 4f, 1f)
        state.tuningZone == TuningZone.FLAT_YELLOW || state.tuningZone == TuningZone.SHARP_YELLOW ->
            Triple(SecondaryAmber, baseThickness + 2f, 1f)
        state.tuningZone == TuningZone.FLAT_RED || state.tuningZone == TuningZone.SHARP_RED ->
            Triple(IncorrectRed, baseThickness + 3f, 1f)
        else ->
            Triple(defaultStringColor, baseThickness, 0.5f)
    }
}
