package com.chordquiz.app.domain.model

enum class NoteDisplayMode {
    NONE,         // ◌
    SHARP,        // C#
    FLAT,         // Bb
    SHARP_OCTAVE, // C#4
    FLAT_OCTAVE;  // Bb4

    fun showNotes(): Boolean = this != NONE
    fun useFlats(): Boolean = this == FLAT || this == FLAT_OCTAVE
    fun showOctave(): Boolean = this == SHARP_OCTAVE || this == FLAT_OCTAVE
}
