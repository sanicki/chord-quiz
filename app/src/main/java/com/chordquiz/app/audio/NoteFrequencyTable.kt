package com.chordquiz.app.audio

import com.chordquiz.app.data.model.Note
import kotlin.math.pow

/**
 * Maps MIDI note numbers to frequencies and vice-versa.
 * A4 (MIDI 69) = 440 Hz.
 */
object NoteFrequencyTable {

    /** Frequency of MIDI note number [midi] (0-127) */
    fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Nearest MIDI note for frequency [hz] */
    fun hzToMidi(hz: Double): Int =
        (12.0 * Math.log(hz / 440.0) / Math.log(2.0) + 69).toInt()

    /** [Note] enum for a given frequency */
    fun hzToNote(hz: Double): Note = Note.fromFrequency(hz)

    /** Minimum guitar frequency (low E2 ≈ 82 Hz) */
    const val MIN_INSTRUMENT_HZ = 60.0

    /** Maximum plausible instrument harmonic (≈4000 Hz) */
    const val MAX_INSTRUMENT_HZ = 4000.0

    /** Frequencies for all guitar open strings, used for sanity-checking detections */
    val GUITAR_OPEN_STRINGS_HZ = doubleArrayOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)
}
