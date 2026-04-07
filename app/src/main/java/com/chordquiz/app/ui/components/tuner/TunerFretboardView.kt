package com.chordquiz.app.ui.components.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordquiz.app.audio.StringTuningState
import com.chordquiz.app.audio.TuningZone
import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.ui.theme.CorrectGreen
import com.chordquiz.app.ui.theme.FretboardWood
import com.chordquiz.app.ui.theme.IncorrectRed
import com.chordquiz.app.ui.theme.NutBrown
import com.chordquiz.app.ui.theme.SecondaryAmber
import com.chordquiz.app.ui.theme.StringColor

/**
 * Horizontal fretboard view for the Tuner screen.
 *
 * Strings run left-to-right (index 0 = lowest/thickest at bottom, matching physical
 * orientation when looking at the fretboard from the front). Open string note names
 * are drawn at the nut on the left. The active string glows in zone color:
 *   - Red (>1 semitone off), Yellow (within 1 semitone), Green (in tune ±20¢).
 * Ambiguous strings glow yellow at 50% alpha.
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
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(fretboardHeight)
    ) {
        val stringCount = instrument.stringCount
        val nutWidth = 36f
        val fretAreaStart = nutWidth + 16f
        val fretAreaEnd = size.width - 16f
        val fretAreaWidth = fretAreaEnd - fretAreaStart

        // Vertical padding so strings don't touch the top/bottom edges
        val verticalPad = size.height * 0.12f
        val usableHeight = size.height - 2 * verticalPad

        // String Y positions — index 0 at bottom (lowest/thickest), index N-1 at top
        val stringYs = List(stringCount) { i ->
            // Reverse: lowest string (index 0) at bottom
            verticalPad + usableHeight * (stringCount - 1 - i) / (stringCount - 1).coerceAtLeast(1)
        }

        // --- Fretboard background ---
        drawRect(
            color = FretboardWood,
            topLeft = Offset(fretAreaStart, verticalPad),
            size = androidx.compose.ui.geometry.Size(fretAreaWidth, usableHeight)
        )

        // --- Nut ---
        drawRect(
            color = NutBrown,
            topLeft = Offset(fretAreaStart, verticalPad - 2f),
            size = androidx.compose.ui.geometry.Size(6f, usableHeight + 4f)
        )

        // --- Fret lines (5 visible frets) ---
        val fretCount = 5
        for (f in 1..fretCount) {
            val x = fretAreaStart + fretAreaWidth * f / fretCount
            drawLine(
                color = Color(0xFFB0BEC5),
                start = Offset(x, verticalPad),
                end = Offset(x, verticalPad + usableHeight),
                strokeWidth = 1.5f
            )
        }

        // --- Strings and note labels ---
        for (i in 0 until stringCount) {
            val state = stringStates.getOrNull(i)
            val y = stringYs[i]

            // String thickness: thicker strings at lower indices (physical guitar feel)
            val baseThickness = 2f + (stringCount - 1 - i) * 0.5f

            val (strokeColor, strokeWidth, alpha) = resolveStringStyle(
                state = state,
                baseThickness = baseThickness
            )

            drawLine(
                color = strokeColor.copy(alpha = alpha),
                start = Offset(fretAreaStart + 6f, y),
                end = Offset(fretAreaEnd, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Note label at the nut
            val label = state?.openNote?.displayName ?: instrument.openStringNotes.getOrNull(i)?.displayName ?: ""
            val labelColor = when {
                state?.isAmbiguous == true -> SecondaryAmber.copy(alpha = 0.7f)
                state?.tuningZone == TuningZone.IN_TUNE -> CorrectGreen
                state?.tuningZone in listOf(TuningZone.FLAT_RED, TuningZone.SHARP_RED) -> IncorrectRed
                state?.tuningZone in listOf(TuningZone.FLAT_YELLOW, TuningZone.SHARP_YELLOW) -> SecondaryAmber
                else -> onSurfaceColor.copy(alpha = 0.7f)
            }

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = labelTextSize
                    color = labelColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.nativeCanvas.drawText(label, nutWidth / 2f, y + labelTextSize / 3f, paint)
            }
        }
    }
}

/** Returns (color, strokeWidth, alpha) for a string based on its tuning state. */
private fun resolveStringStyle(
    state: StringTuningState?,
    baseThickness: Float
): Triple<Color, Float, Float> {
    if (state == null) return Triple(StringColor, baseThickness, 0.4f)

    return when {
        state.isAmbiguous -> Triple(SecondaryAmber, baseThickness + 2f, 0.5f)
        state.tuningZone == TuningZone.IN_TUNE -> Triple(CorrectGreen, baseThickness + 4f, 1f)
        state.tuningZone == TuningZone.FLAT_YELLOW || state.tuningZone == TuningZone.SHARP_YELLOW ->
            Triple(SecondaryAmber, baseThickness + 2f, 1f)
        state.tuningZone == TuningZone.FLAT_RED || state.tuningZone == TuningZone.SHARP_RED ->
            Triple(IncorrectRed, baseThickness + 3f, 1f)
        else -> Triple(StringColor, baseThickness, 0.5f)
    }
}
