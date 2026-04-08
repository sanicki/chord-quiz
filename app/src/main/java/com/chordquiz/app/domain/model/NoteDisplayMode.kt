package com.chordquiz.app.domain.model

enum class NoteDisplayMode {
    NONE,               // ◌
    OPEN_STRINGS_ONLY,  // | open string names above nut only
    SHARP,              // C#
    FLAT,               // Bb
    SHARP_OCTAVE,       // C#4
    FLAT_OCTAVE;        // Bb4

    fun showNotes(): Boolean = this != NONE && this != OPEN_STRINGS_ONLY
    fun showOpenStringsOnly(): Boolean = this == OPEN_STRINGS_ONLY
    fun useFlats(): Boolean = this == FLAT || this == FLAT_OCTAVE
    fun showOctave(): Boolean = this == SHARP_OCTAVE || this == FLAT_OCTAVE
}
