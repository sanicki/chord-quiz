package com.chordquiz.app.audio

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Note
import javax.inject.Inject
import javax.inject.Singleton

data class RecognitionResult(
    val chord: ChordDefinition,
    val detectedNotes: Set<Note>,
    val confidence: Float  // 0.0 – 1.0
)

@Singleton
class ChordRecognizer @Inject constructor() {

    private var candidateChords: List<ChordDefinition> = emptyList()

    /** Set the pool of chords to match against (the selected quiz chords) */
    fun setCandidates(chords: List<ChordDefinition>) {
        candidateChords = chords
    }

    /**
     * Given a list of detected fundamental frequencies, return the best-matching
     * chord from [candidateChords] (or null if confidence < 0.4).
     */
    fun recognize(detectedFrequencies: List<Double>): RecognitionResult? {
        if (detectedFrequencies.isEmpty() || candidateChords.isEmpty()) return null

        val detectedNotes = detectedFrequencies
            .map { NoteFrequencyTable.hzToNote(it) }
            .toSet()

        var bestChord: ChordDefinition? = null
        var bestScore = 0f

        for (chord in candidateChords) {
            val score = computeScore(detectedNotes, chord.noteComponents.toSet())
            if (score > bestScore) {
                bestScore = score
                bestChord = chord
            }
        }

        if (bestScore < 0.4f || bestChord == null) return null

        return RecognitionResult(
            chord = bestChord,
            detectedNotes = detectedNotes,
            confidence = bestScore
        )
    }

    /**
     * Score = |intersection| / |target|, penalized by extra notes.
     * Extra notes outside chord tones reduce score slightly.
     */
    private fun computeScore(detected: Set<Note>, target: Set<Note>): Float {
        val intersection = detected.intersect(target)
        val coverage = intersection.size.toFloat() / target.size.toFloat()
        val extraPenalty = (detected.size - intersection.size).toFloat() * 0.05f
        return (coverage - extraPenalty).coerceIn(0f, 1f)
    }
}
