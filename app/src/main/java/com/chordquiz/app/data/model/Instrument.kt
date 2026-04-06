package com.chordquiz.app.data.model

data class Instrument(
    val id: String,
    val displayName: String,
    val stringCount: Int,
    /** Open string notes, index 0 = lowest-pitched (thickest) string */
    val openStringNotes: List<Note>,
    /** MIDI octave per string (e.g. E2 = octave 2) */
    val openStringOctaves: List<Int>,
    val displayedFretCount: Int = 5,
    val totalFrets: Int = 21
) {
    companion object {
        val GUITAR = Instrument(
            id = "guitar_standard",
            displayName = "Guitar",
            stringCount = 6,
            openStringNotes = listOf(Note.E, Note.A, Note.D, Note.G, Note.B, Note.E),
            openStringOctaves = listOf(2, 2, 3, 3, 3, 4),
            totalFrets = 20
        )
        val UKULELE = Instrument(
            id = "ukulele_soprano",
            displayName = "Ukulele",
            stringCount = 4,
            openStringNotes = listOf(Note.G, Note.C, Note.E, Note.A),
            openStringOctaves = listOf(4, 4, 4, 4),
            totalFrets = 12
        )
        val BASS = Instrument(
            id = "bass_standard",
            displayName = "Bass",
            stringCount = 4,
            openStringNotes = listOf(Note.E, Note.A, Note.D, Note.G),
            openStringOctaves = listOf(1, 1, 2, 2),
            totalFrets = 24
        )
        val BANJO = Instrument(
            id = "banjo_5string",
            displayName = "Banjo",
            stringCount = 5,
            // String 0 = 5th string (high G, short string), strings 1-4 = D G B D low to high
            openStringNotes = listOf(Note.G, Note.D, Note.G, Note.B, Note.D),
            openStringOctaves = listOf(4, 3, 3, 3, 4),
            totalFrets = 22
        )

        val ALL = listOf(GUITAR, BASS, UKULELE, BANJO)
    }
}
