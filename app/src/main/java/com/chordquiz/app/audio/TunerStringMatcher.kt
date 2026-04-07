package com.chordquiz.app.audio

import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.Note
import kotlin.math.abs
import kotlin.math.log2

enum class TuningZone { NEUTRAL, FLAT_RED, FLAT_YELLOW, IN_TUNE, SHARP_YELLOW, SHARP_RED }

data class StringTuningState(
    val stringIndex: Int,
    val openNote: Note,
    val octave: Int,
    val targetHz: Float,
    /** Cents deviation from target (negative = flat, positive = sharp). Null when NEUTRAL. */
    val centsDeviation: Float?,
    val tuningZone: TuningZone,
    /** True when two or more strings are both within [AMBIGUITY_CENTS] of the detected pitch. */
    val isAmbiguous: Boolean = false
)

/**
 * Maps a detected frequency to the nearest open string(s) of an instrument and
 * returns per-string [StringTuningState] for rendering and guidance text.
 */
object TunerStringMatcher {

    /** Cents range for the green "in tune" zone. */
    private const val IN_TUNE_CENTS = 20f

    /** Cents range for the yellow zone (one semitone = 100 cents). */
    private const val YELLOW_CENTS = 100f

    /**
     * When two or more strings are both within this many cents of the detected
     * frequency the result is considered ambiguous and both are flagged.
     */
    private const val AMBIGUITY_CENTS = 50f

    /**
     * Returns the tuning state for every string in [instrument] based on [detectedHz].
     *
     * If [detectedHz] is null all strings are NEUTRAL (no signal detected).
     */
    fun match(detectedHz: Float?, instrument: Instrument): List<StringTuningState> {
        val targetFreqs = instrument.openStringNotes.mapIndexed { i, note ->
            val octave = instrument.openStringOctaves[i]
            val midi = note.semitone + 12 * (octave + 1)
            NoteFrequencyTable.midiToHz(midi).toFloat()
        }

        if (detectedHz == null || detectedHz <= 0f) {
            return instrument.openStringNotes.mapIndexed { i, note ->
                StringTuningState(
                    stringIndex = i,
                    openNote = note,
                    octave = instrument.openStringOctaves[i],
                    targetHz = targetFreqs[i],
                    centsDeviation = null,
                    tuningZone = TuningZone.NEUTRAL
                )
            }
        }

        // Compute cents deviation for each string
        val deviations = targetFreqs.map { target ->
            if (target > 0f) 1200f * log2(detectedHz / target) else Float.MAX_VALUE
        }

        // Strings within AMBIGUITY_CENTS are all "candidates"
        val candidates = deviations.indices.filter { abs(deviations[it]) <= AMBIGUITY_CENTS }
        val isAmbiguous = candidates.size >= 2

        // Index of the nearest string (used when not ambiguous)
        val nearestIndex = deviations.indices.minByOrNull { abs(deviations[it]) } ?: 0

        return instrument.openStringNotes.mapIndexed { i, note ->
            val dev = deviations[i]
            val isCandidateForAmbig = isAmbiguous && i in candidates
            val isNearest = !isAmbiguous && i == nearestIndex

            val activeDev: Float?
            val zone: TuningZone
            val ambiguous: Boolean

            when {
                isCandidateForAmbig -> {
                    activeDev = dev
                    zone = TuningZone.NEUTRAL
                    ambiguous = true
                }
                isNearest -> {
                    activeDev = dev
                    ambiguous = false
                    zone = when {
                        abs(dev) <= IN_TUNE_CENTS -> TuningZone.IN_TUNE
                        abs(dev) <= YELLOW_CENTS -> if (dev < 0f) TuningZone.FLAT_YELLOW else TuningZone.SHARP_YELLOW
                        else -> if (dev < 0f) TuningZone.FLAT_RED else TuningZone.SHARP_RED
                    }
                }
                else -> {
                    activeDev = null
                    zone = TuningZone.NEUTRAL
                    ambiguous = false
                }
            }

            StringTuningState(
                stringIndex = i,
                openNote = note,
                octave = instrument.openStringOctaves[i],
                targetHz = targetFreqs[i],
                centsDeviation = activeDev,
                tuningZone = zone,
                isAmbiguous = ambiguous
            )
        }
    }
}
