package com.chordquiz.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val WoodLight  = Color(0xFFD4A96A)
private val WoodDark   = Color(0xFF8B5A2B)
private val WoodMid    = Color(0xFFB07838)
private val StringCol  = Color(0xFFD0D0C0)
private val MetalCol   = Color(0xFFB8B8B8)
private val HoleColor  = Color(0xFF3A2510)

@Composable
fun InstrumentIcon(
    instrumentId: String,
    modifier: Modifier = Modifier.size(48.dp, 64.dp)
) {
    Canvas(modifier = modifier) {
        when (instrumentId) {
            "guitar_standard" -> drawAcousticGuitar()
            "ukulele_soprano" -> drawUkulele()
            "bass_standard"   -> drawAcousticBass()
            "banjo_5string"   -> drawBanjo()
            else              -> drawAcousticGuitar()
        }
    }
}

// ---------------------------------------------------------------------------
// Acoustic Guitar (6-string, 3+3 headstock, round sound hole)
// ---------------------------------------------------------------------------
private fun DrawScope.drawAcousticGuitar() {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // Neck
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.07f, h * 0.06f),
        size = Size(w * 0.14f, h * 0.40f)
    )

    // Headstock (wider than neck, flat top)
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.15f, 0f),
        size = Size(w * 0.30f, h * 0.09f)
    )

    // Tuning pegs: 3 left, 3 right on headstock
    val pegY1 = h * 0.015f
    val pegY2 = h * 0.045f
    val pegY3 = h * 0.075f
    for (pegY in listOf(pegY1, pegY2, pegY3)) {
        drawCircle(MetalCol, w * 0.035f, Offset(cx - w * 0.18f, pegY))
        drawCircle(MetalCol, w * 0.035f, Offset(cx + w * 0.18f, pegY))
    }

    // Guitar body path (double-bout shape)
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.36f,
        bodyBottom = h * 0.98f,
        upperHalfW = w * 0.32f,
        lowerHalfW = w * 0.44f,
        waistHalfW = w * 0.24f,
        color = WoodLight
    )

    // Binding (outline)
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.36f,
        bodyBottom = h * 0.98f,
        upperHalfW = w * 0.32f,
        lowerHalfW = w * 0.44f,
        waistHalfW = w * 0.24f,
        color = WoodDark,
        strokeWidth = 2f
    )

    // Sound hole
    drawCircle(HoleColor, w * 0.10f, Offset(cx, h * 0.72f))
    drawCircle(WoodDark, w * 0.10f, Offset(cx, h * 0.72f), style = Stroke(1.5f))

    // Bridge
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.14f, h * 0.86f),
        size = Size(w * 0.28f, h * 0.04f)
    )

    // Strings (6) over neck and body
    val stringXStart = cx - w * 0.06f
    val stringSpacing = w * 0.024f
    for (i in 0..5) {
        val sx = stringXStart + i * stringSpacing
        drawLine(StringCol, Offset(sx, h * 0.06f), Offset(sx, h * 0.88f), 0.8f)
    }

    // Nut line
    drawLine(MetalCol, Offset(cx - w * 0.07f, h * 0.40f), Offset(cx + w * 0.07f, h * 0.40f), 2f)
}

// ---------------------------------------------------------------------------
// Ukulele (4-string, rounder body, 2+2 headstock)
// ---------------------------------------------------------------------------
private fun DrawScope.drawUkulele() {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // Neck
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.06f, h * 0.07f),
        size = Size(w * 0.12f, h * 0.36f)
    )

    // Headstock
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.12f, 0f),
        size = Size(w * 0.24f, h * 0.09f)
    )

    // Tuning pegs: 2 left, 2 right
    val pegY1 = h * 0.02f
    val pegY2 = h * 0.06f
    for (pegY in listOf(pegY1, pegY2)) {
        drawCircle(MetalCol, w * 0.03f, Offset(cx - w * 0.16f, pegY))
        drawCircle(MetalCol, w * 0.03f, Offset(cx + w * 0.16f, pegY))
    }

    // Ukulele body: rounder, more symmetrical
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.38f,
        bodyBottom = h * 0.97f,
        upperHalfW = w * 0.36f,
        lowerHalfW = w * 0.42f,
        waistHalfW = w * 0.27f,
        color = WoodLight
    )
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.38f,
        bodyBottom = h * 0.97f,
        upperHalfW = w * 0.36f,
        lowerHalfW = w * 0.42f,
        waistHalfW = w * 0.27f,
        color = WoodDark,
        strokeWidth = 2f
    )

    // Sound hole
    drawCircle(HoleColor, w * 0.09f, Offset(cx, h * 0.71f))

    // Bridge
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.12f, h * 0.84f),
        size = Size(w * 0.24f, h * 0.035f)
    )

    // Strings (4)
    val stringXStart = cx - w * 0.045f
    val stringSpacing = w * 0.03f
    for (i in 0..3) {
        val sx = stringXStart + i * stringSpacing
        drawLine(StringCol, Offset(sx, h * 0.07f), Offset(sx, h * 0.855f), 0.8f)
    }

    drawLine(MetalCol, Offset(cx - w * 0.06f, h * 0.40f), Offset(cx + w * 0.06f, h * 0.40f), 2f)
}

// ---------------------------------------------------------------------------
// Acoustic Bass (4-string, wide body, f-holes, 2+2 headstock)
// ---------------------------------------------------------------------------
private fun DrawScope.drawAcousticBass() {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // Neck (slightly wider, longer)
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.08f, h * 0.04f),
        size = Size(w * 0.16f, h * 0.42f)
    )

    // Headstock
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.14f, 0f),
        size = Size(w * 0.28f, h * 0.08f)
    )

    // Tuning pegs: 2+2
    for (pegY in listOf(h * 0.015f, h * 0.055f)) {
        drawCircle(MetalCol, w * 0.035f, Offset(cx - w * 0.18f, pegY))
        drawCircle(MetalCol, w * 0.035f, Offset(cx + w * 0.18f, pegY))
    }

    // Bass body: deeper, wider than guitar
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.35f,
        bodyBottom = h * 0.97f,
        upperHalfW = w * 0.38f,
        lowerHalfW = w * 0.46f,
        waistHalfW = w * 0.26f,
        color = WoodLight
    )
    drawGuitarBodyPath(
        cx = cx,
        bodyTop = h * 0.35f,
        bodyBottom = h * 0.97f,
        upperHalfW = w * 0.38f,
        lowerHalfW = w * 0.46f,
        waistHalfW = w * 0.26f,
        color = WoodDark,
        strokeWidth = 2f
    )

    // F-holes (two narrow ovals, one each side of center)
    val fHoleW = w * 0.06f
    val fHoleH = h * 0.12f
    val fHoleY = h * 0.66f
    // Left f-hole
    drawFHole(cx - w * 0.18f, fHoleY, fHoleW, fHoleH)
    // Right f-hole
    drawFHole(cx + w * 0.18f, fHoleY, fHoleW, fHoleH)

    // Bridge
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.15f, h * 0.84f),
        size = Size(w * 0.30f, h * 0.04f)
    )

    // Strings (4)
    val stringXStart = cx - w * 0.045f
    val stringSpacing = w * 0.03f
    for (i in 0..3) {
        val sx = stringXStart + i * stringSpacing
        drawLine(StringCol, Offset(sx, h * 0.04f), Offset(sx, h * 0.86f), 1f)
    }

    drawLine(MetalCol, Offset(cx - w * 0.08f, h * 0.42f), Offset(cx + w * 0.08f, h * 0.42f), 2f)
}

private fun DrawScope.drawFHole(cx: Float, centerY: Float, fw: Float, fh: Float) {
    // Simplified f-hole: thin oval
    val path = Path().apply {
        addOval(Rect(cx - fw / 2f, centerY - fh / 2f, cx + fw / 2f, centerY + fh / 2f))
    }
    drawPath(path, HoleColor)
    // Notches at mid-height
    drawLine(WoodDark, Offset(cx - fw * 0.7f, centerY), Offset(cx + fw * 0.7f, centerY), 1.5f)
}

// ---------------------------------------------------------------------------
// Banjo (circular drum head + resonator ring, neck with 5th peg)
// ---------------------------------------------------------------------------
private fun DrawScope.drawBanjo() {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // Neck
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.07f, h * 0.04f),
        size = Size(w * 0.14f, h * 0.38f)
    )

    // Headstock (narrow, longer)
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.10f, 0f),
        size = Size(w * 0.20f, h * 0.07f)
    )

    // Tuning pegs: 2+2 on headstock + 1 on neck side (5th string peg)
    for (pegY in listOf(h * 0.01f, h * 0.045f)) {
        drawCircle(MetalCol, w * 0.03f, Offset(cx - w * 0.15f, pegY))
        drawCircle(MetalCol, w * 0.03f, Offset(cx + w * 0.15f, pegY))
    }
    // 5th string peg on the side of the neck
    drawCircle(MetalCol, w * 0.025f, Offset(cx + w * 0.12f, h * 0.20f))

    // Resonator ring (outer circle)
    val drumCY = h * 0.72f
    val drumR = w * 0.43f
    drawCircle(WoodMid, drumR, Offset(cx, drumCY))
    drawCircle(WoodDark, drumR, Offset(cx, drumCY), style = Stroke(3f))

    // Inner resonator ring
    drawCircle(WoodDark, drumR * 0.88f, Offset(cx, drumCY), style = Stroke(1.5f))

    // Membrane (drum head)
    drawCircle(Color(0xFFF5EFDE), drumR * 0.82f, Offset(cx, drumCY))

    // Bridge on membrane
    drawRect(
        color = WoodDark,
        topLeft = Offset(cx - w * 0.06f, drumCY - h * 0.03f),
        size = Size(w * 0.12f, h * 0.025f)
    )

    // Strings (5) over neck
    val stringXStart = cx - w * 0.06f
    val stringSpacing = w * 0.03f
    for (i in 0..4) {
        val sx = stringXStart + i * stringSpacing
        val topY = if (i == 0) h * 0.20f else h * 0.04f  // 5th string starts lower
        drawLine(StringCol, Offset(sx, topY), Offset(sx, drumCY - h * 0.02f), 0.8f)
    }

    drawLine(MetalCol, Offset(cx - w * 0.07f, h * 0.40f), Offset(cx + w * 0.07f, h * 0.40f), 2f)
}

// ---------------------------------------------------------------------------
// Shared helper: draw a guitar-style double-bout body as a filled path
// Pass strokeWidth > 0 to draw as outline instead of filled
// ---------------------------------------------------------------------------
private fun DrawScope.drawGuitarBodyPath(
    cx: Float,
    bodyTop: Float,
    bodyBottom: Float,
    upperHalfW: Float,
    lowerHalfW: Float,
    waistHalfW: Float,
    color: Color,
    strokeWidth: Float = 0f
) {
    val bh = bodyBottom - bodyTop
    val waistY = bodyTop + bh * 0.44f
    val upperCY = bodyTop + bh * 0.22f
    val lowerCY = bodyTop + bh * 0.72f

    val path = Path().apply {
        moveTo(cx, bodyTop)
        // Right: top → upper bout
        cubicTo(cx + upperHalfW * 0.6f, bodyTop, cx + upperHalfW, upperCY - bh * 0.08f, cx + upperHalfW, upperCY)
        // Right: upper bout → waist
        cubicTo(cx + upperHalfW, upperCY + bh * 0.10f, cx + waistHalfW, waistY - bh * 0.04f, cx + waistHalfW, waistY)
        // Right: waist → lower bout
        cubicTo(cx + waistHalfW, waistY + bh * 0.04f, cx + lowerHalfW, lowerCY - bh * 0.10f, cx + lowerHalfW, lowerCY)
        // Right: lower bout → bottom
        cubicTo(cx + lowerHalfW, lowerCY + bh * 0.10f, cx + lowerHalfW * 0.5f, bodyBottom, cx, bodyBottom)
        // Left: bottom → lower bout
        cubicTo(cx - lowerHalfW * 0.5f, bodyBottom, cx - lowerHalfW, lowerCY + bh * 0.10f, cx - lowerHalfW, lowerCY)
        // Left: lower bout → waist
        cubicTo(cx - lowerHalfW, lowerCY - bh * 0.10f, cx - waistHalfW, waistY + bh * 0.04f, cx - waistHalfW, waistY)
        // Left: waist → upper bout
        cubicTo(cx - waistHalfW, waistY - bh * 0.04f, cx - upperHalfW, upperCY + bh * 0.10f, cx - upperHalfW, upperCY)
        // Left: upper bout → top
        cubicTo(cx - upperHalfW, upperCY - bh * 0.08f, cx - upperHalfW * 0.6f, bodyTop, cx, bodyTop)
        close()
    }

    if (strokeWidth > 0f) {
        drawPath(path, color, style = Stroke(strokeWidth))
    } else {
        drawPath(path, color)
    }
}
