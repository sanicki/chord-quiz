package com.chordquiz.app.data.model

data class ChordDefinition(
    val id: String,
    val instrumentId: String,
    val chordName: String,
    val rootNote: Note,
    val chordType: ChordType,
    /** Multiple voicings; index 0 is the canonical/preferred one */
    val fingerings: List<Fingering>,
    /** Notes that constitute this chord (root, third, fifth, etc.) */
    val noteComponents: List<Note>
) {
    val displayName: String get() = "${rootNote.displayName}${chordType.suffix}"
}
