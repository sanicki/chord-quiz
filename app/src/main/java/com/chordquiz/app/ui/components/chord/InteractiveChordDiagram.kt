package com.chordquiz.app.ui.components.chord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordquiz.app.data.model.BarreSegment
import com.chordquiz.app.data.model.Fingering
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.StringPosition
import com.chordquiz.app.domain.model.NoteDisplayMode
import com.chordquiz.app.ui.theme.BarreColor
import com.chordquiz.app.ui.theme.FingerDot
import com.chordquiz.app.ui.theme.IncorrectRed
import com.chordquiz.app.ui.theme.MutedGray
import com.chordquiz.app.ui.theme.NutBrown
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.sqrt

// Height of each fret row in the LazyColumn (shows ~4-5 at a time in a 280 dp container).
private val FRET_ITEM_HEIGHT = 52.dp

// Height of the fixed above-nut row that is always visible above the scrolling area.
private val NUT_AREA_HEIGHT = 36.dp

// Horizontal padding fractions (relative to composable width).
private const val LEFT_PAD_FRAC = 0.12f   // space for fret-number label
private const val RIGHT_PAD_FRAC = 0.04f

// Fraction of row height used as the dot/symbol radius — shared by FretItem for all rows.
private const val DOT_RADIUS_FRAC = 0.33f

/** Returns the x-centre pixel of [stringIndex] for a composable of [width] px. */
private fun stringX(stringIndex: Int, width: Float, stringCount: Int): Float {
    val leftPad = width * LEFT_PAD_FRAC
    val strArea = width * (1f - LEFT_PAD_FRAC - RIGHT_PAD_FRAC)
    val spacing = strArea / (stringCount - 1).coerceAtLeast(1)
    return leftPad + stringIndex * spacing
}

/** Returns the nearest string index for touch position [x] in a composable of [width] px. */
private fun stringIndexAt(x: Float, width: Float, stringCount: Int): Int {
    val leftPad = width * LEFT_PAD_FRAC
    val strArea = width * (1f - LEFT_PAD_FRAC - RIGHT_PAD_FRAC)
    val spacing = strArea / (stringCount - 1).coerceAtLeast(1)
    return ((x - leftPad) / spacing).toInt().coerceIn(0, stringCount - 1)
}

/**
 * Tappable chord diagram backed by a [LazyColumn] for smooth native scroll and fling.
 *
 * - **Scroll**: LazyColumn handles vertical fling; [rememberSnapFlingBehavior] snaps to
 *   the nearest fret wire after a fling, just like scrolling a web page.
 * - **Tap**: Each fret row uses [awaitEachGesture] and only fires [onFingeringChanged] when the
 *   finger lifts without exceeding the system touch-slop — so a scroll never places a note.
 * - **Barre**: A right-to-left drag within a single fret row is classified as a barre gesture
 *   (once horizontal movement exceeds touch-slop); the LazyColumn sees it as non-vertical and
 *   does not scroll.
 * - **Background**: White to match the Play Quiz screen.
 * - **Rendering**: Static grid lines (fret wires + strings) are drawn with [drawWithCache], so
 *   the geometry is cached across recompositions and only recomputed on size changes.
 *
 * @param noteQuizMode    When `true`: disables mute/barre; open-string area toggles fret=0 dots.
 * @param hintPositions   (stringIndex, fret) pairs rendered as yellow hint dots.
 * @param onNoteSelected  Called when a finger dot is placed (fret >= 0).
 */
@Composable
fun InteractiveChordDiagram(
    stringCount: Int,
    totalFrets: Int = 21,
    initialFingering: Fingering? = null,
    noteQuizMode: Boolean = false,
    hintPositions: Set<Pair<Int, Int>> = emptySet(),
    incorrectFrettedStrings: Set<Int> = emptySet(),
    incorrectMutedStrings: Set<Int> = emptySet(),
    missedMuteStrings: Set<Int> = emptySet(),
    openStringNotes: List<Note> = emptyList(),
    openStringOctaves: List<Int> = emptyList(),
    noteDisplayMode: NoteDisplayMode = NoteDisplayMode.NONE,
    onFingeringChanged: (Fingering) -> Unit,
    onNoteSelected: ((stringIndex: Int, fret: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val initialPositions = initialFingering?.positions
        ?: (0 until stringCount).map { StringPosition(it, 0) }
    var positions by remember(stringCount) { mutableStateOf(initialPositions.toMutableList()) }
    var barre by remember(stringCount) { mutableStateOf(initialFingering?.barre) }

    // Barre drag bookkeeping: snapshot of positions at drag-start for rubber-band reversal.
    var barreDragBase by remember { mutableStateOf<List<StringPosition>?>(null) }
    var barreDragStartString by remember { mutableStateOf(0) }

    val initialScroll = ((initialFingering?.baseFret ?: 1) - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScroll)
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val textMeasurer = rememberTextMeasurer()

    // Propagate scroll-position changes to the parent once scrolling settles.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
            .filter { (_, scrolling) -> !scrolling }
            .map { (index, _) -> index }
            .distinctUntilChanged()
            .collect { index ->
                onFingeringChanged(Fingering(positions.toList(), barre, index + 1))
            }
    }

    // ── Shared tap handler ──────────────────────────────────────────────────
    fun handleFretTap(stringIndex: Int, fretNumber: Int) {
        val curBarre = barre
        val tapOnBarre = curBarre != null &&
            stringIndex in curBarre.fromString..curBarre.toString &&
            fretNumber == curBarre.fret

        if (tapOnBarre) {
            val isEndpoint = stringIndex == curBarre!!.fromString ||
                stringIndex == curBarre.toString
            if (isEndpoint) {
                val newFrom = if (stringIndex == curBarre.fromString)
                    curBarre.fromString + 1 else curBarre.fromString
                val newTo = if (stringIndex == curBarre.toString)
                    curBarre.toString - 1 else curBarre.toString
                barre = if (newTo - newFrom >= 1)
                    BarreSegment(curBarre.fret, newFrom, newTo) else null
                positions = positions.toMutableList().also { list ->
                    val idx = list.indexOfFirst { it.stringIndex == stringIndex }
                    if (idx >= 0) list[idx] = StringPosition(stringIndex, 0)
                }
            } else {
                barre = null
                positions = positions.toMutableList().also { list ->
                    for (s in curBarre!!.fromString..curBarre.toString) {
                        val idx = list.indexOfFirst { it.stringIndex == s }
                        if (idx >= 0) list[idx] = StringPosition(s, 0)
                    }
                }
            }
            onFingeringChanged(Fingering(positions.toList(), barre, listState.firstVisibleItemIndex + 1))
        } else {
            val curPos = positions.firstOrNull { it.stringIndex == stringIndex }
            val emptyFret = if (noteQuizMode) -1 else 0
            val newFret = if (curPos?.fret == fretNumber) emptyFret else fretNumber
            positions = positions.toMutableList().also { list ->
                val idx = list.indexOfFirst { it.stringIndex == stringIndex }
                if (idx >= 0) list[idx] = StringPosition(stringIndex, newFret)
                else list.add(StringPosition(stringIndex, newFret))
            }
            onFingeringChanged(
                Fingering(positions.toList(), if (noteQuizMode) null else barre,
                    listState.firstVisibleItemIndex + 1)
            )
            if (newFret > 0) onNoteSelected?.invoke(stringIndex, newFret)
        }
    }

    fun handleAboveNutTap(stringIndex: Int) {
        if (noteQuizMode) {
            val cur = positions.firstOrNull { it.stringIndex == stringIndex }?.fret ?: -1
            val newFret = if (cur == 0) -1 else 0
            positions = positions.toMutableList().also { list ->
                val idx = list.indexOfFirst { it.stringIndex == stringIndex }
                if (idx >= 0) list[idx] = StringPosition(stringIndex, newFret)
            }
            onFingeringChanged(
                Fingering(positions.toList(), null, listState.firstVisibleItemIndex + 1)
            )
            if (newFret == 0) onNoteSelected?.invoke(stringIndex, 0)
        } else {
            val cur = positions.firstOrNull { it.stringIndex == stringIndex }?.fret ?: 0
            val newFret = if (cur == -1) 0 else -1
            positions = positions.toMutableList().also { list ->
                val idx = list.indexOfFirst { it.stringIndex == stringIndex }
                if (idx >= 0) list[idx] = StringPosition(stringIndex, newFret)
            }
            onFingeringChanged(
                Fingering(positions.toList(), barre, listState.firstVisibleItemIndex + 1)
            )
            if (newFret == 0) onNoteSelected?.invoke(stringIndex, 0)
        }
    }

    // ── Layout ──────────────────────────────────────────────────────────────
    Column(modifier = modifier) {

        // Fixed nut row (fret 0) with open/muted markers (always visible, never scrolls).
        FretItem(
            fretNumber = 0,
            stringCount = stringCount,
            positions = positions,
            barre = null,
            hintPositions = emptySet(),
            incorrectFrettedStrings = incorrectFrettedStrings,
            incorrectMutedStrings = incorrectMutedStrings,
            missedMuteStrings = missedMuteStrings,
            noteQuizMode = noteQuizMode,
            noteDisplayMode = noteDisplayMode,
            openStringNotes = openStringNotes,
            openStringOctaves = openStringOctaves,
            textMeasurer = textMeasurer,
            onTap = { stringIndex -> handleAboveNutTap(stringIndex) },
            onBarreDragStart = {},
            onBarreDragUpdate = {},
            onBarreDragEnd = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(NUT_AREA_HEIGHT)
        )

        // Scrollable fret area — only visible items are composed.
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(totalFrets) { fretIndex ->
                val fretNumber = fretIndex + 1
                FretItem(
                    fretNumber = fretNumber,
                    stringCount = stringCount,
                    positions = positions,
                    barre = barre,
                    hintPositions = hintPositions,
                    incorrectFrettedStrings = incorrectFrettedStrings,
                    noteQuizMode = noteQuizMode,
                    noteDisplayMode = noteDisplayMode,
                    openStringNotes = openStringNotes,
                    openStringOctaves = openStringOctaves,
                    textMeasurer = textMeasurer,
                    onTap = { stringIndex -> handleFretTap(stringIndex, fretNumber) },
                    onBarreDragStart = { startString ->
                        barreDragStartString = startString
                        val curBarre = barre
                        barreDragBase = if (curBarre != null) {
                            positions.map { pos ->
                                if (pos.stringIndex in curBarre.fromString..curBarre.toString)
                                    StringPosition(pos.stringIndex, 0)
                                else pos
                            }
                        } else positions.toList()
                        barre = null
                        positions = (barreDragBase ?: positions.toList()).toMutableList()
                    },
                    onBarreDragUpdate = { currentString ->
                        val base = barreDragBase
                        if (base != null) {
                            val barreEnd = minOf(currentString, barreDragStartString)
                            if (barreEnd < barreDragStartString) {
                                val span = barreEnd..barreDragStartString
                                positions = base.map { pos ->
                                    if (pos.stringIndex in span && pos.fret <= fretNumber)
                                        StringPosition(pos.stringIndex, fretNumber)
                                    else pos
                                }.toMutableList()
                                val absorbed = span.filter { s ->
                                    (base.firstOrNull { it.stringIndex == s }?.fret ?: 0) <= fretNumber
                                }
                                barre = if (absorbed.size >= 2)
                                    BarreSegment(fretNumber, absorbed.min(), absorbed.max())
                                else null
                            } else {
                                barre = null
                                positions = base.toMutableList()
                            }
                        }
                    },
                    onBarreDragEnd = {
                        val finalBarre = barre
                        if (finalBarre != null) {
                            // Barre committed — propagate to parent.
                            onFingeringChanged(
                                Fingering(positions.toList(), finalBarre,
                                    listState.firstVisibleItemIndex + 1)
                            )
                        } else {
                            // Drag produced no barre (e.g. reversed back to start string) →
                            // restore positions and treat the gesture as a tap.
                            val base = barreDragBase
                            if (base != null) {
                                positions = base.toMutableList()
                            }
                            handleFretTap(barreDragStartString, fretNumber)
                        }
                        barreDragBase = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FRET_ITEM_HEIGHT)
                )
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

/**
 * One horizontal fret band in the LazyColumn.
 *
 * Static grid (fret wire + string lines) is drawn with [drawWithCache] so the geometry
 * is computed once per size change and reused on every recomposition.
 *
 * Dynamic content (finger dots, barre, hint dots, note labels) is drawn in a Canvas
 * layered on top.
 *
 * Gesture handling:
 * - Movement ≤ touch-slop → [onTap] fires (note placement / barre shrink / removal).
 * - Right-to-left horizontal movement > touch-slop → barre drag ([onBarreDragStart] /
 *   [onBarreDragUpdate] / [onBarreDragEnd]).
 * - Left-to-right or vertical movement > touch-slop → not consumed; the [LazyColumn]
 *   handles vertical scroll; the parent's swipe-to-submit handles left-to-right.
 */
@Composable
private fun FretItem(
    fretNumber: Int,
    stringCount: Int,
    positions: List<StringPosition>,
    barre: BarreSegment?,
    hintPositions: Set<Pair<Int, Int>>,
    incorrectFrettedStrings: Set<Int>,
    incorrectMutedStrings: Set<Int> = emptySet(),
    missedMuteStrings: Set<Int> = emptySet(),
    noteQuizMode: Boolean,
    noteDisplayMode: NoteDisplayMode,
    openStringNotes: List<Note>,
    openStringOctaves: List<Int>,
    textMeasurer: TextMeasurer,
    onTap: (stringIndex: Int) -> Unit,
    onBarreDragStart: (startString: Int) -> Unit,
    onBarreDragUpdate: (currentString: Int) -> Unit,
    onBarreDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            // ── Static grid (cached) ──────────────────────────────────────
            .drawWithCache {
                val leftPad  = size.width * LEFT_PAD_FRAC
                val strArea  = size.width * (1f - LEFT_PAD_FRAC - RIGHT_PAD_FRAC)
                val strSpacing = strArea / (stringCount - 1).coerceAtLeast(1)
                // Pre-compute string x-positions once; reused every draw pass.
                val strXs = List(stringCount) { s -> leftPad + s * strSpacing }

                onDrawBehind {
                    if (fretNumber == 0) {
                        // String lines stop at nut bar
                        val nutY = size.height - 5f
                        for (x in strXs) {
                            drawLine(onSurface, Offset(x, 0f), Offset(x, nutY), 1.5f)
                        }
                        // Nut bar drawn on top of strings
                        drawRect(
                            color = NutBrown,
                            topLeft = Offset(leftPad, nutY),
                            size = Size(strArea, 5f)
                        )
                    } else {
                        // Fret wire at top of this row
                        drawLine(
                            color = onSurface.copy(alpha = 0.4f),
                            start = Offset(leftPad, 0f),
                            end = Offset(leftPad + strArea, 0f),
                            strokeWidth = 1.5f
                        )
                        // String lines (vertical, full row height)
                        for (x in strXs) {
                            drawLine(onSurface, Offset(x, 0f), Offset(x, size.height), 1.5f)
                        }
                    }
                }
            }
            // ── Gesture handling ──────────────────────────────────────────
            .pointerInput(fretNumber, stringCount, noteQuizMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val startString = stringIndexAt(startX, size.width.toFloat(), stringCount)

                    var classified      = false
                    var barreDragActive = false

                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        val dx   = change.position.x - startX
                        val dy   = change.position.y - startY
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        if (!classified && dist > viewConfiguration.touchSlop) {
                            classified = true
                            val isHorizontal = abs(dx) >= abs(dy)
                            when {
                                !isHorizontal -> break  // vertical → LazyColumn scrolls
                                dx < 0 && !noteQuizMode && fretNumber > 0 -> {
                                    // Right-to-left horizontal → barre drag
                                    barreDragActive = true
                                    onBarreDragStart(startString)
                                }
                                else -> break  // left-to-right → swipe-to-submit in parent
                            }
                        }

                        if (barreDragActive) {
                            change.consume()
                            onBarreDragUpdate(stringIndexAt(change.position.x, size.width.toFloat(), stringCount))
                        }

                        if (!change.pressed) {
                            when {
                                !classified -> {
                                    val cx = stringX(startString, size.width.toFloat(), stringCount)
                                    val dotR = size.height * DOT_RADIUS_FRAC
                                    val midY = size.height / 2f
                                    val tapDist = sqrt(
                                        ((startX - cx) * (startX - cx) + (startY - midY) * (startY - midY)).toDouble()
                                    ).toFloat()
                                    if (tapDist <= dotR) onTap(startString)
                                }
                                barreDragActive -> onBarreDragEnd()
                            }
                            break
                        }
                    }
                }
            }
    ) {
        // ── Dynamic content ────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad    = size.width * LEFT_PAD_FRAC
            val strArea    = size.width * (1f - LEFT_PAD_FRAC - RIGHT_PAD_FRAC)
            val strSpacing = strArea / (stringCount - 1).coerceAtLeast(1)
            val midY       = size.height / 2f
            val dotRadius  = size.height * DOT_RADIUS_FRAC

            // Fret number label (left margin) — hidden for fret 0 (nut row)
            if (fretNumber > 0) {
                val labelText  = fretNumber.toString()
                val labelStyle = TextStyle(
                    color     = onSurface.copy(alpha = 0.6f),
                    fontSize  = (size.height * 0.32f / density).sp
                )
                val labelMeasured = textMeasurer.measure(labelText, style = labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text          = labelText,
                    topLeft       = Offset(
                        x = (leftPad - labelMeasured.size.width) / 2f,
                        y = midY - labelMeasured.size.height / 2f
                    ),
                    style = labelStyle
                )
            }

            // Tap-target hint circles (very light, behind dots)
            for (s in 0 until stringCount) {
                val cx = leftPad + s * strSpacing
                drawCircle(Color.Gray.copy(alpha = 0.10f), dotRadius, Offset(cx, midY))
            }

            // Finger dots (for fretted notes; fret 0 in chord mode uses open/muted markers below)
            positions.filter { it.fret == fretNumber && (fretNumber > 0 || noteQuizMode) }.forEach { pos ->
                val x        = leftPad + pos.stringIndex * strSpacing
                val dotColor = if (pos.stringIndex in incorrectFrettedStrings) IncorrectRed else FingerDot
                drawCircle(dotColor, dotRadius, Offset(x, midY))
            }

            // Barre (drawn after finger dots so barre covers them)
            barre?.takeIf { it.fret == fretNumber }?.let { b ->
                val x1 = leftPad + b.fromString * strSpacing
                val x2 = leftPad + b.toString  * strSpacing
                // Calculate the outer edges of the barre line - encompassing the first and last finger dots
                val startX = x1 - dotRadius
                val endX = x2 + dotRadius

                // Check if any finger dots beneath this barre are red (incorrect)
                val barreColorToUse = if (positions.any {
                    it.fret == fretNumber &&
                    it.stringIndex in b.fromString..b.toString &&
                    it.stringIndex in incorrectFrettedStrings
                }) {
                    IncorrectRed
                } else {
                    BarreColor
                }

                drawRoundRect(
                    color      = barreColorToUse,
                    topLeft    = Offset(startX, midY - dotRadius),
                    size       = Size(endX - startX, dotRadius * 2),
                    cornerRadius = CornerRadius(dotRadius, dotRadius)
                )
            }

            // Open/muted markers for fret 0 row in chord mode (open circle, muted X)
            if (fretNumber == 0 && !noteQuizMode) {
                positions.forEach { pos ->
                    val x = leftPad + pos.stringIndex * strSpacing
                    when (pos.fret) {
                        -1 -> {
                            val col = if (pos.stringIndex in incorrectMutedStrings) IncorrectRed else MutedGray
                            drawLine(col, Offset(x - dotRadius, midY - dotRadius), Offset(x + dotRadius, midY + dotRadius), 2f)
                            drawLine(col, Offset(x + dotRadius, midY - dotRadius), Offset(x - dotRadius, midY + dotRadius), 2f)
                        }
                        0 -> {
                            val col = if (pos.stringIndex in missedMuteStrings || pos.stringIndex in incorrectFrettedStrings)
                                IncorrectRed else onSurface
                            if (noteDisplayMode.showOpenStringsOnly()
                                && pos.stringIndex < openStringNotes.size
                                && pos.stringIndex < openStringOctaves.size) {
                                val note = openStringNotes[pos.stringIndex]
                                val label = note.displayNameFor(noteDisplayMode)
                                val labelStyle = TextStyle(
                                    color = col.copy(alpha = 0.85f),
                                    fontSize = (dotRadius * 2.2f / density).sp
                                )
                                val measured = textMeasurer.measure(label, style = labelStyle)
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = label,
                                    topLeft = Offset(x - measured.size.width / 2f, midY - measured.size.height / 2f),
                                    style = labelStyle
                                )
                            } else {
                                drawCircle(col, dotRadius, Offset(x, midY), style = Stroke(2f))
                            }
                        }
                    }
                }
            }

            // Hint dots (yellow)
            hintPositions.filter { (_, f) -> f == fretNumber }.forEach { (s, _) ->
                val x = leftPad + s * strSpacing
                drawCircle(Color(0xFFFFCC00), dotRadius, Offset(x, midY))
            }

            // Note labels (when display mode is active)
            if (noteDisplayMode.showNotes() && openStringNotes.isNotEmpty() && openStringOctaves.isNotEmpty()) {
                val noteFontSize = (size.height * 0.26f / density).sp
                val styleOnDark  = TextStyle(color = Color.White,          fontSize = noteFontSize)
                val styleOnLight = TextStyle(color = Color(0xFF888888),    fontSize = noteFontSize)

                for (s in 0 until stringCount) {
                    val openNote   = openStringNotes.getOrNull(s)   ?: continue
                    val openOctave = openStringOctaves.getOrNull(s) ?: continue
                    val note       = openNote.plus(fretNumber)
                    val octave     = openOctave + (openNote.semitone + fretNumber) / 12
                    val hasFingerDot = positions.any { it.stringIndex == s && it.fret == fretNumber }
                    val isInBarre    = barre?.let { b ->
                        fretNumber == b.fret && s in b.fromString..b.toString
                    } ?: false

                    val label   = note.displayNameFor(
                        noteDisplayMode, octave.takeIf { noteDisplayMode.showOctave() }
                    )
                    val style   = if (hasFingerDot || isInBarre) styleOnDark else styleOnLight
                    val cx      = leftPad + s * strSpacing
                    val measured = textMeasurer.measure(label, style = style)
                    drawText(
                        textMeasurer = textMeasurer,
                        text          = label,
                        topLeft       = Offset(cx - measured.size.width / 2f, midY - measured.size.height / 2f),
                        style         = style
                    )
                }
            }
        }
    }
}
