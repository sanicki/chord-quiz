package com.chordquiz.app.ui.components.chord

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText

fun DrawScope.drawMutedX(center: Offset, radius: Float, color: Color, strokeWidth: Float = 2f) {
    drawLine(color, Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius), strokeWidth)
    drawLine(color, Offset(center.x + radius, center.y - radius), Offset(center.x - radius, center.y + radius), strokeWidth)
}

fun DrawScope.drawOpenCircle(center: Offset, radius: Float, color: Color, strokeWidth: Float = 2f) {
    drawCircle(color, radius, center, style = Stroke(strokeWidth))
}

fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    centerX: Float,
    centerY: Float,
    style: TextStyle,
) {
    val measured = textMeasurer.measure(text, style = style)
    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(centerX - measured.size.width / 2f, centerY - measured.size.height / 2f),
        style = style,
    )
}
