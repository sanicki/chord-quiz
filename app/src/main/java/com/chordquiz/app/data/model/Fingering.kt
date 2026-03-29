package com.chordquiz.app.data.model

/**
 * Position of a finger on a single string.
 * fret = 0 means open string; fret = -1 means muted (X).
 * stringIndex: 0 = lowest-pitched (thickest) string.
 */
data class StringPosition(
    val stringIndex: Int,
    val fret: Int
)

/** A barre chord segment spanning multiple strings at the same fret. */
data class BarreSegment(
    val fret: Int,
    val fromString: Int,  // inclusive, 0-based
    val toString: Int     // inclusive
)

/**
 * Complete fingering (voicing) for a chord.
 * [positions] must have one entry per string (including muted strings).
 * [baseFret] is the lowest fret shown — diagrams show [baseFret]..[baseFret+4].
 */
data class Fingering(
    val positions: List<StringPosition>,
    val barre: BarreSegment? = null,
    val baseFret: Int = 1,
    val fingerLabels: Map<Int, Int> = emptyMap()  // stringIndex -> finger (1-4)
)
