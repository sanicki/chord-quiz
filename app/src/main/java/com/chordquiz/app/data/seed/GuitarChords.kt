package com.chordquiz.app.data.seed

import com.chordquiz.app.data.model.*

/**
 * Guitar chord library — standard EADGBE tuning.
 * Strings: 0=E2, 1=A2, 2=D3, 3=G3, 4=B3, 5=E4
 * fret=-1 means muted, fret=0 means open.
 */
object GuitarChords {

    private const val ID = "guitar_standard"

    private fun pos(vararg frets: Int): List<StringPosition> =
        frets.mapIndexed { i, f -> StringPosition(i, f) }

    private fun chord(
        root: Note,
        type: ChordType,
        vararg fingeringData: Pair<List<StringPosition>, BarreSegment?>
    ): ChordDefinition {
        val notes = type.intervals.map { root.plus(it) }
        return ChordDefinition(
            id = "${ID}_${root.displayName}${type.suffix}",
            instrumentId = ID,
            chordName = "${root.displayName}${type.suffix}",
            rootNote = root,
            chordType = type,
            fingerings = fingeringData.map { (positions, barre) ->
                val fretted = positions.filter { it.fret > 0 }
                val baseFret = if ((fretted.maxOfOrNull { it.fret } ?: 0) > 5)
                    fretted.minOfOrNull { it.fret } ?: 1 else 1
                Fingering(positions = positions, barre = barre, baseFret = baseFret)
            },
            noteComponents = notes
        )
    }

    // ── MAJOR ────────────────────────────────────────────────────────────────

    val A_MAJOR = chord(Note.A, ChordType.MAJOR,
        pos(-1, 0, 2, 2, 2, 0) to null,
        pos(5, 7, 7, 6, 5, 5) to BarreSegment(5, 0, 5)
    )
    val A_SHARP_MAJOR = chord(Note.A_SHARP, ChordType.MAJOR,
        pos(-1, 1, 3, 3, 3, 1) to BarreSegment(1, 1, 5)
    )
    val B_MAJOR = chord(Note.B, ChordType.MAJOR,
        pos(-1, 2, 4, 4, 4, 2) to BarreSegment(2, 1, 5)
    )
    val C_MAJOR = chord(Note.C, ChordType.MAJOR,
        pos(-1, 3, 2, 0, 1, 0) to null,
        pos(8, 10, 10, 9, 8, 8) to BarreSegment(8, 0, 5)
    )
    val C_SHARP_MAJOR = chord(Note.C_SHARP, ChordType.MAJOR,
        pos(-1, 4, 6, 6, 6, 4) to BarreSegment(4, 1, 5)
    )
    val D_MAJOR = chord(Note.D, ChordType.MAJOR,
        pos(-1, -1, 0, 2, 3, 2) to null,
        pos(-1, 5, 7, 7, 7, 5) to BarreSegment(5, 1, 5)
    )
    val D_SHARP_MAJOR = chord(Note.D_SHARP, ChordType.MAJOR,
        pos(-1, 6, 8, 8, 8, 6) to BarreSegment(6, 1, 5)
    )
    val E_MAJOR = chord(Note.E, ChordType.MAJOR,
        pos(0, 2, 2, 1, 0, 0) to null
    )
    val F_MAJOR = chord(Note.F, ChordType.MAJOR,
        pos(1, 1, 2, 3, 3, 1) to BarreSegment(1, 0, 5)
    )
    val F_SHARP_MAJOR = chord(Note.F_SHARP, ChordType.MAJOR,
        pos(2, 2, 4, 4, 4, 2) to BarreSegment(2, 0, 5)
    )
    val G_MAJOR = chord(Note.G, ChordType.MAJOR,
        pos(3, 2, 0, 0, 0, 3) to null,
        pos(3, 5, 5, 4, 3, 3) to BarreSegment(3, 0, 5)
    )
    val G_SHARP_MAJOR = chord(Note.G_SHARP, ChordType.MAJOR,
        pos(4, 6, 6, 5, 4, 4) to BarreSegment(4, 0, 5)
    )

    // ── MINOR ────────────────────────────────────────────────────────────────

    val A_MINOR = chord(Note.A, ChordType.MINOR,
        pos(-1, 0, 2, 2, 1, 0) to null
    )
    val A_SHARP_MINOR = chord(Note.A_SHARP, ChordType.MINOR,
        pos(-1, 1, 3, 3, 2, 1) to BarreSegment(1, 1, 5)
    )
    val B_MINOR = chord(Note.B, ChordType.MINOR,
        pos(-1, 2, 4, 4, 3, 2) to BarreSegment(2, 1, 5)
    )
    val C_MINOR = chord(Note.C, ChordType.MINOR,
        pos(-1, 3, 5, 5, 4, 3) to BarreSegment(3, 1, 5)
    )
    val C_SHARP_MINOR = chord(Note.C_SHARP, ChordType.MINOR,
        pos(-1, 4, 6, 6, 5, 4) to BarreSegment(4, 1, 5)
    )
    val D_MINOR = chord(Note.D, ChordType.MINOR,
        pos(-1, -1, 0, 2, 3, 1) to null
    )
    val D_SHARP_MINOR = chord(Note.D_SHARP, ChordType.MINOR,
        pos(-1, 6, 8, 8, 7, 6) to BarreSegment(6, 1, 5)
    )
    val E_MINOR = chord(Note.E, ChordType.MINOR,
        pos(0, 2, 2, 0, 0, 0) to null
    )
    val F_MINOR = chord(Note.F, ChordType.MINOR,
        pos(1, 1, 3, 3, 2, 1) to BarreSegment(1, 0, 5)
    )
    val F_SHARP_MINOR = chord(Note.F_SHARP, ChordType.MINOR,
        pos(2, 2, 4, 4, 3, 2) to BarreSegment(2, 0, 5)
    )
    val G_MINOR = chord(Note.G, ChordType.MINOR,
        pos(3, 5, 5, 3, 3, 3) to BarreSegment(3, 0, 5)
    )
    val G_SHARP_MINOR = chord(Note.G_SHARP, ChordType.MINOR,
        pos(4, 6, 6, 4, 4, 4) to BarreSegment(4, 0, 5)
    )

    // ── DOMINANT 7 ───────────────────────────────────────────────────────────

    val A_DOM7 = chord(Note.A, ChordType.DOMINANT_7,
        pos(-1, 0, 2, 0, 2, 0) to null
    )
    val A_SHARP_DOM7 = chord(Note.A_SHARP, ChordType.DOMINANT_7,
        pos(-1, 1, 3, 1, 3, 1) to BarreSegment(1, 1, 5)
    )
    val B_DOM7 = chord(Note.B, ChordType.DOMINANT_7,
        pos(-1, 2, 4, 2, 4, 2) to BarreSegment(2, 1, 5)
    )
    val C_DOM7 = chord(Note.C, ChordType.DOMINANT_7,
        pos(-1, 3, 2, 3, 1, 0) to null
    )
    val C_SHARP_DOM7 = chord(Note.C_SHARP, ChordType.DOMINANT_7,
        pos(-1, 4, 6, 4, 6, 4) to BarreSegment(4, 1, 5)
    )
    val D_DOM7 = chord(Note.D, ChordType.DOMINANT_7,
        pos(-1, -1, 0, 2, 1, 2) to null
    )
    val D_SHARP_DOM7 = chord(Note.D_SHARP, ChordType.DOMINANT_7,
        pos(-1, 6, 8, 6, 8, 6) to BarreSegment(6, 1, 5)
    )
    val E_DOM7 = chord(Note.E, ChordType.DOMINANT_7,
        pos(0, 2, 0, 1, 0, 0) to null
    )
    val F_DOM7 = chord(Note.F, ChordType.DOMINANT_7,
        pos(1, 1, 3, 1, 3, 1) to BarreSegment(1, 0, 5)
    )
    val F_SHARP_DOM7 = chord(Note.F_SHARP, ChordType.DOMINANT_7,
        pos(2, 2, 4, 2, 4, 2) to BarreSegment(2, 0, 5)
    )
    val G_DOM7 = chord(Note.G, ChordType.DOMINANT_7,
        pos(3, 2, 0, 0, 0, 1) to null
    )
    val G_SHARP_DOM7 = chord(Note.G_SHARP, ChordType.DOMINANT_7,
        pos(4, 6, 4, 5, 4, 4) to BarreSegment(4, 0, 5)
    )

    // ── MAJOR 7 ──────────────────────────────────────────────────────────────

    val A_MAJ7 = chord(Note.A, ChordType.MAJOR_7,
        pos(-1, 0, 2, 1, 2, 0) to null
    )
    val A_SHARP_MAJ7 = chord(Note.A_SHARP, ChordType.MAJOR_7,
        pos(-1, 1, 3, 2, 3, 1) to BarreSegment(1, 1, 5)
    )
    val B_MAJ7 = chord(Note.B, ChordType.MAJOR_7,
        pos(-1, 2, 4, 3, 4, 2) to BarreSegment(2, 1, 5)
    )
    val C_MAJ7 = chord(Note.C, ChordType.MAJOR_7,
        pos(-1, 3, 2, 0, 0, 0) to null
    )
    val C_SHARP_MAJ7 = chord(Note.C_SHARP, ChordType.MAJOR_7,
        pos(-1, 4, 6, 5, 6, 4) to BarreSegment(4, 1, 5)
    )
    val D_MAJ7 = chord(Note.D, ChordType.MAJOR_7,
        pos(-1, -1, 0, 2, 2, 2) to null
    )
    val D_SHARP_MAJ7 = chord(Note.D_SHARP, ChordType.MAJOR_7,
        pos(-1, 6, 8, 7, 8, 6) to BarreSegment(6, 1, 5)
    )
    val E_MAJ7 = chord(Note.E, ChordType.MAJOR_7,
        pos(0, 2, 1, 1, 0, 0) to null
    )
    val F_MAJ7 = chord(Note.F, ChordType.MAJOR_7,
        pos(-1, -1, 3, 2, 1, 0) to null
    )
    val F_SHARP_MAJ7 = chord(Note.F_SHARP, ChordType.MAJOR_7,
        pos(2, 4, 3, 3, 2, 2) to BarreSegment(2, 0, 5)
    )
    val G_MAJ7 = chord(Note.G, ChordType.MAJOR_7,
        pos(3, 2, 0, 0, 0, 2) to null
    )
    val G_SHARP_MAJ7 = chord(Note.G_SHARP, ChordType.MAJOR_7,
        pos(4, 6, 5, 5, 4, 4) to BarreSegment(4, 0, 5)
    )

    // ── MINOR 7 ──────────────────────────────────────────────────────────────

    val A_MIN7 = chord(Note.A, ChordType.MINOR_7,
        pos(-1, 0, 2, 0, 1, 0) to null
    )
    val A_SHARP_MIN7 = chord(Note.A_SHARP, ChordType.MINOR_7,
        pos(-1, 1, 3, 1, 2, 1) to BarreSegment(1, 1, 5)
    )
    val B_MIN7 = chord(Note.B, ChordType.MINOR_7,
        pos(-1, 2, 4, 2, 3, 2) to BarreSegment(2, 1, 5)
    )
    val C_MIN7 = chord(Note.C, ChordType.MINOR_7,
        pos(-1, 3, 5, 3, 4, 3) to BarreSegment(3, 1, 5)
    )
    val C_SHARP_MIN7 = chord(Note.C_SHARP, ChordType.MINOR_7,
        pos(-1, 4, 6, 4, 5, 4) to BarreSegment(4, 1, 5)
    )
    val D_MIN7 = chord(Note.D, ChordType.MINOR_7,
        pos(-1, -1, 0, 2, 1, 1) to null
    )
    val D_SHARP_MIN7 = chord(Note.D_SHARP, ChordType.MINOR_7,
        pos(-1, 6, 8, 6, 7, 6) to BarreSegment(6, 1, 5)
    )
    val E_MIN7 = chord(Note.E, ChordType.MINOR_7,
        pos(0, 2, 0, 0, 0, 0) to null
    )
    val F_MIN7 = chord(Note.F, ChordType.MINOR_7,
        pos(1, 1, 3, 1, 2, 1) to BarreSegment(1, 0, 5)
    )
    val F_SHARP_MIN7 = chord(Note.F_SHARP, ChordType.MINOR_7,
        pos(2, 2, 4, 2, 3, 2) to BarreSegment(2, 0, 5)
    )
    val G_MIN7 = chord(Note.G, ChordType.MINOR_7,
        pos(3, 5, 3, 3, 3, 3) to BarreSegment(3, 0, 5)
    )
    val G_SHARP_MIN7 = chord(Note.G_SHARP, ChordType.MINOR_7,
        pos(4, 6, 4, 4, 4, 4) to BarreSegment(4, 0, 5)
    )

    val ALL: List<ChordDefinition> = listOf(
        A_MAJOR, A_SHARP_MAJOR, B_MAJOR, C_MAJOR, C_SHARP_MAJOR, D_MAJOR,
        D_SHARP_MAJOR, E_MAJOR, F_MAJOR, F_SHARP_MAJOR, G_MAJOR, G_SHARP_MAJOR,
        A_MINOR, A_SHARP_MINOR, B_MINOR, C_MINOR, C_SHARP_MINOR, D_MINOR,
        D_SHARP_MINOR, E_MINOR, F_MINOR, F_SHARP_MINOR, G_MINOR, G_SHARP_MINOR,
        A_DOM7, A_SHARP_DOM7, B_DOM7, C_DOM7, C_SHARP_DOM7, D_DOM7,
        D_SHARP_DOM7, E_DOM7, F_DOM7, F_SHARP_DOM7, G_DOM7, G_SHARP_DOM7,
        A_MAJ7, A_SHARP_MAJ7, B_MAJ7, C_MAJ7, C_SHARP_MAJ7, D_MAJ7,
        D_SHARP_MAJ7, E_MAJ7, F_MAJ7, F_SHARP_MAJ7, G_MAJ7, G_SHARP_MAJ7,
        A_MIN7, A_SHARP_MIN7, B_MIN7, C_MIN7, C_SHARP_MIN7, D_MIN7,
        D_SHARP_MIN7, E_MIN7, F_MIN7, F_SHARP_MIN7, G_MIN7, G_SHARP_MIN7
    )
}
