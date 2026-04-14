package com.chordquiz.app.ui.components.chord

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawMutedX(center: Offset, radius: Float, color: Color, strokeWidth: Float = 2f) {
    drawLine(color, Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius), strokeWidth)
    drawLine(color, Offset(center.x + radius, center.y - radius), Offset(center.x - radius, center.y + radius), strokeWidth)
}

fun DrawScope.drawOpenCircle(center: Offset, radius: Float, color: Color, strokeWidth: Float = 2f) {
    drawCircle(color, radius, center, style = Stroke(strokeWidth))
}
