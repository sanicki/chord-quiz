package com.chordquiz.app.data.seed

import com.chordquiz.app.data.model.*

/**
 * Ukulele chord library — concert soprano GCEA tuning.
 * Strings: 0=G4, 1=C4, 2=E4, 3=A4
 */
object UkuleleChords {

    private const val ID = "ukulele_soprano"

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
    val A_MAJOR  = chord(Note.A,       ChordType.MAJOR, pos(2,1,0,0) to null)
    val AS_MAJOR = chord(Note.A_SHARP, ChordType.MAJOR, pos(3,2,1,1) to BarreSegment(1,2,3))
    val B_MAJOR  = chord(Note.B,       ChordType.MAJOR, pos(4,3,2,2) to BarreSegment(2,2,3))
    val C_MAJOR  = chord(Note.C,       ChordType.MAJOR, pos(0,0,0,3) to null)
    val CS_MAJOR = chord(Note.C_SHARP, ChordType.MAJOR, pos(1,1,1,4) to BarreSegment(1,0,2))
    val D_MAJOR  = chord(Note.D,       ChordType.MAJOR, pos(2,2,2,0) to null)
    val DS_MAJOR = chord(Note.D_SHARP, ChordType.MAJOR, pos(3,3,3,1) to null)
    val E_MAJOR  = chord(Note.E,       ChordType.MAJOR, pos(4,4,4,2) to null)
    val F_MAJOR  = chord(Note.F,       ChordType.MAJOR, pos(2,0,1,0) to null)
    val FS_MAJOR = chord(Note.F_SHARP, ChordType.MAJOR, pos(3,1,2,1) to null)
    val G_MAJOR  = chord(Note.G,       ChordType.MAJOR, pos(0,2,3,2) to null)
    val GS_MAJOR = chord(Note.G_SHARP, ChordType.MAJOR, pos(5,3,4,3) to null)

    // ── MINOR ────────────────────────────────────────────────────────────────
    val A_MINOR  = chord(Note.A,       ChordType.MINOR, pos(2,0,0,0) to null)
    val AS_MINOR = chord(Note.A_SHARP, ChordType.MINOR, pos(3,1,1,1) to BarreSegment(1,1,3))
    val B_MINOR  = chord(Note.B,       ChordType.MINOR, pos(4,2,2,2) to BarreSegment(2,1,3))
    val C_MINOR  = chord(Note.C,       ChordType.MINOR, pos(0,3,3,3) to null)
    val CS_MINOR = chord(Note.C_SHARP, ChordType.MINOR, pos(1,4,4,4) to null)
    val D_MINOR  = chord(Note.D,       ChordType.MINOR, pos(2,2,1,0) to null)
    val DS_MINOR = chord(Note.D_SHARP, ChordType.MINOR, pos(3,3,2,1) to null)
    val E_MINOR  = chord(Note.E,       ChordType.MINOR, pos(0,4,3,2) to null)
    val F_MINOR  = chord(Note.F,       ChordType.MINOR, pos(1,0,1,3) to null)
    val FS_MINOR = chord(Note.F_SHARP, ChordType.MINOR, pos(2,1,2,0) to null)
    val G_MINOR  = chord(Note.G,       ChordType.MINOR, pos(0,2,3,1) to null)
    val GS_MINOR = chord(Note.G_SHARP, ChordType.MINOR, pos(4,3,4,2) to null)

    // ── DOMINANT 7 ───────────────────────────────────────────────────────────
    val A_DOM7  = chord(Note.A,       ChordType.DOMINANT_7, pos(0,1,0,0) to null)
    val AS_DOM7 = chord(Note.A_SHARP, ChordType.DOMINANT_7, pos(1,2,1,1) to BarreSegment(1,0,3))
    val B_DOM7  = chord(Note.B,       ChordType.DOMINANT_7, pos(2,3,2,2) to BarreSegment(2,0,3))
    val C_DOM7  = chord(Note.C,       ChordType.DOMINANT_7, pos(0,0,0,1) to null)
    val CS_DOM7 = chord(Note.C_SHARP, ChordType.DOMINANT_7, pos(1,1,1,2) to null)
    val D_DOM7  = chord(Note.D,       ChordType.DOMINANT_7, pos(2,2,2,3) to null)
    val DS_DOM7 = chord(Note.D_SHARP, ChordType.DOMINANT_7, pos(3,3,3,4) to null)
    val E_DOM7  = chord(Note.E,       ChordType.DOMINANT_7, pos(1,2,0,2) to null)
    val F_DOM7  = chord(Note.F,       ChordType.DOMINANT_7, pos(2,3,1,3) to null)
    val FS_DOM7 = chord(Note.F_SHARP, ChordType.DOMINANT_7, pos(3,4,2,4) to null)
    val G_DOM7  = chord(Note.G,       ChordType.DOMINANT_7, pos(0,2,1,2) to null)
    val GS_DOM7 = chord(Note.G_SHARP, ChordType.DOMINANT_7, pos(1,3,2,3) to null)

    // ── MAJOR 7 ──────────────────────────────────────────────────────────────
    val A_MAJ7  = chord(Note.A,       ChordType.MAJOR_7, pos(1,1,0,0) to null)
    val AS_MAJ7 = chord(Note.A_SHARP, ChordType.MAJOR_7, pos(2,2,1,1) to null)
    val B_MAJ7  = chord(Note.B,       ChordType.MAJOR_7, pos(3,3,2,2) to null)
    val C_MAJ7  = chord(Note.C,       ChordType.MAJOR_7, pos(0,0,0,2) to null)
    val CS_MAJ7 = chord(Note.C_SHARP, ChordType.MAJOR_7, pos(1,1,1,3) to null)
    val D_MAJ7  = chord(Note.D,       ChordType.MAJOR_7, pos(2,2,2,4) to null)
    val DS_MAJ7 = chord(Note.D_SHARP, ChordType.MAJOR_7, pos(3,3,3,5) to null)
    val E_MAJ7  = chord(Note.E,       ChordType.MAJOR_7, pos(1,3,0,2) to null)
    val F_MAJ7  = chord(Note.F,       ChordType.MAJOR_7, pos(2,4,1,3) to null)
    val FS_MAJ7 = chord(Note.F_SHARP, ChordType.MAJOR_7, pos(3,5,2,4) to null)
    val G_MAJ7  = chord(Note.G,       ChordType.MAJOR_7, pos(0,2,2,2) to null)
    val GS_MAJ7 = chord(Note.G_SHARP, ChordType.MAJOR_7, pos(5,3,3,3) to null)

    // ── MINOR 7 ──────────────────────────────────────────────────────────────
    val A_MIN7  = chord(Note.A,       ChordType.MINOR_7, pos(0,0,0,0) to null)
    val AS_MIN7 = chord(Note.A_SHARP, ChordType.MINOR_7, pos(1,1,1,1) to BarreSegment(1,0,3))
    val B_MIN7  = chord(Note.B,       ChordType.MINOR_7, pos(2,2,2,2) to BarreSegment(2,0,3))
    val C_MIN7  = chord(Note.C,       ChordType.MINOR_7, pos(0,3,3,3) to null)
    val CS_MIN7 = chord(Note.C_SHARP, ChordType.MINOR_7, pos(1,4,4,4) to null)
    val D_MIN7  = chord(Note.D,       ChordType.MINOR_7, pos(2,2,1,3) to null)
    val DS_MIN7 = chord(Note.D_SHARP, ChordType.MINOR_7, pos(3,3,2,4) to null)
    val E_MIN7  = chord(Note.E,       ChordType.MINOR_7, pos(0,2,0,2) to null)
    val F_MIN7  = chord(Note.F,       ChordType.MINOR_7, pos(1,3,1,3) to null)
    val FS_MIN7 = chord(Note.F_SHARP, ChordType.MINOR_7, pos(2,4,2,4) to null)
    val G_MIN7  = chord(Note.G,       ChordType.MINOR_7, pos(0,2,1,1) to null)
    val GS_MIN7 = chord(Note.G_SHARP, ChordType.MINOR_7, pos(1,3,2,2) to null)

    val ALL: List<ChordDefinition> = listOf(
        A_MAJOR, AS_MAJOR, B_MAJOR, C_MAJOR, CS_MAJOR, D_MAJOR,
        DS_MAJOR, E_MAJOR, F_MAJOR, FS_MAJOR, G_MAJOR, GS_MAJOR,
        A_MINOR, AS_MINOR, B_MINOR, C_MINOR, CS_MINOR, D_MINOR,
        DS_MINOR, E_MINOR, F_MINOR, FS_MINOR, G_MINOR, GS_MINOR,
        A_DOM7, AS_DOM7, B_DOM7, C_DOM7, CS_DOM7, D_DOM7,
        DS_DOM7, E_DOM7, F_DOM7, FS_DOM7, G_DOM7, GS_DOM7,
        A_MAJ7, AS_MAJ7, B_MAJ7, C_MAJ7, CS_MAJ7, D_MAJ7,
        DS_MAJ7, E_MAJ7, F_MAJ7, FS_MAJ7, G_MAJ7, GS_MAJ7,
        A_MIN7, AS_MIN7, B_MIN7, C_MIN7, CS_MIN7, D_MIN7,
        DS_MIN7, E_MIN7, F_MIN7, FS_MIN7, G_MIN7, GS_MIN7
    )
}
