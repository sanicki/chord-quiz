package com.chordquiz.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class NoteMode {
    FIND_NOTE,
    FIND_NOTE_CORRECT_OCTAVE,
    FIND_ALL_NOTES,
    FIND_ALL_NOTES_CORRECT_OCTAVE;

    val displayName: String
        get() = when (this) {
            FIND_NOTE -> "Find the note"
            FIND_NOTE_CORRECT_OCTAVE -> "Find the note in the correct octave"
            FIND_ALL_NOTES -> "Find all the notes"
            FIND_ALL_NOTES_CORRECT_OCTAVE -> "Find all the notes in the correct octave"
        }
}
