package com.chordquiz.app.data.model

import com.chordquiz.app.domain.model.NoteDisplayMode
import kotlin.math.roundToInt

enum class Note(val semitone: Int, val displayName: String, val flatName: String? = null) {
    C(0, "C"),
    C_SHARP(1, "C#", "Db"),
    D(2, "D"),
    D_SHARP(3, "D#", "Eb"),
    E(4, "E"),
    F(5, "F"),
    F_SHARP(6, "F#", "Gb"),
    G(7, "G"),
    G_SHARP(8, "G#", "Ab"),
    A(9, "A"),
    A_SHARP(10, "A#", "Bb"),
    B(11, "B");

    fun plus(semitones: Int): Note = fromSemitone(semitone + semitones)

    fun displayNameFor(mode: NoteDisplayMode, octave: Int? = null): String {
        val base = if (mode.useFlats()) flatName ?: displayName else displayName
        return if (mode.showOctave() && octave != null) "$base$octave" else base
    }

    companion object {
        fun fromSemitone(s: Int): Note = entries[s.mod(12)]

        fun fromFrequency(hz: Double): Note {
            val midi = (12.0 * Math.log(hz / 440.0) / Math.log(2.0) + 69).roundToInt()
            return fromSemitone(midi)
        }

        fun fromName(name: String): Note? = entries.find {
            it.displayName.equals(name, ignoreCase = true) ||
                it.flatName?.equals(name, ignoreCase = true) == true
        }
    }
}
