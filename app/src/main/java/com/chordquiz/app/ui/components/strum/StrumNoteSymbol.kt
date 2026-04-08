package com.chordquiz.app.ui.components.strum

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.chordquiz.app.ui.screen.strumpractice.StrumNote

/**
 * Draws a musical note symbol on a Canvas for the given [noteType].
 *
 * - Whole: open oval head, no stem
 * - Half: open oval head + stem
 * - Quarter: filled oval head + stem
 * - Eighth: filled oval head + stem + flag
 *
 * Drawing style mirrors MusicalGradeStaff.kt (drawOval / drawLine on Canvas).
 */
@Composable
fun StrumNoteSymbol(
    noteType: StrumNote,
    modifier: Modifier = Modifier.size(width = 20.dp, height = 40.dp)
) {
    val inkColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val ovalCx     = size.width / 2f
        val ovalCy     = size.height * 0.78f
        val rx         = size.width * 0.42f
        val ry         = size.height * 0.14f
        val stemX      = ovalCx + rx
        val stemTop    = size.height * 0.08f
        val stemBottom = ovalCy - ry
        val strokeWidth = 2f

        // Oval head
        val ovalTopLeft = Offset(ovalCx - rx, ovalCy - ry)
        val ovalSize    = Size(rx * 2f, ry * 2f)

        when (noteType) {
            StrumNote.WHOLE -> {
                drawOval(
                    color   = inkColor,
                    topLeft = ovalTopLeft,
                    size    = ovalSize,
                    style   = Stroke(width = strokeWidth)
                )
            }
            StrumNote.HALF -> {
                drawOval(
                    color   = inkColor,
                    topLeft = ovalTopLeft,
                    size    = ovalSize,
                    style   = Stroke(width = strokeWidth)
                )
                drawLine(inkColor, Offset(stemX, stemBottom), Offset(stemX, stemTop), strokeWidth = strokeWidth)
            }
            StrumNote.QUARTER -> {
                drawOval(color = inkColor, topLeft = ovalTopLeft, size = ovalSize)
                drawLine(inkColor, Offset(stemX, stemBottom), Offset(stemX, stemTop), strokeWidth = strokeWidth)
            }
            StrumNote.EIGHTH -> {
                drawOval(color = inkColor, topLeft = ovalTopLeft, size = ovalSize)
                drawLine(inkColor, Offset(stemX, stemBottom), Offset(stemX, stemTop), strokeWidth = strokeWidth)
                // Flag: diagonal line from stem top angling right and down
                drawLine(
                    color       = inkColor,
                    start       = Offset(stemX, stemTop),
                    end         = Offset(stemX + size.width * 0.5f, stemTop + size.height * 0.25f),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
