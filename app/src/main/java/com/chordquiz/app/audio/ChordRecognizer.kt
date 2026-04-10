package com.chordquiz.app.audio

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Note
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

data class RecognitionResult(
    val chord: ChordDefinition,
    val detectedNotes: Set<Note>,
    val confidence: Float  // 0.0 – 1.0
)

@Singleton
class ChordRecognizer @Inject constructor() {

    private var candidateChords: List<ChordDefinition> = emptyList()

    /**
     * Rolling window of the last [WINDOW_SIZE] recognized chord IDs (null = no match).
     * A result is only emitted when all frames in the window agree on the same chord.
     */
    private val recentChordIds = ArrayDeque<String?>()

    /** Cache for chord note components to avoid recomputation */
    private val chordComponentCache = mutableMapOf<String, Set<Note>>()

    companion object {
        private const val WINDOW_SIZE = 4
        private const val CHROMA_THRESHOLD = 0.15
        private const val MIN_CONFIDENCE = 0.4f
    }

    /** Set the pool of chords to match against (the selected quiz chords). Resets the buffer. */
    fun setCandidates(chords: List<ChordDefinition>) {
        candidateChords = chords
        resetBuffer()
        // Clear the cache when candidates change
        chordComponentCache.clear()
    }

    /** Clear the rolling recognition buffer — call between questions to avoid carry-over. */
    fun resetBuffer() {
        recentChordIds.clear()
    }

    /**
     * Get chord note components with caching to avoid recomputation.
     */
    private fun getChordComponents(chord: ChordDefinition): Set<Note> {
        return chordComponentCache.getOrPut(chord.id) {
            chord.noteComponents.toSet()
        }
    }

    /**
     * Given a list of detected frequency bins (frequency + whitened magnitude from [PitchDetector]),
     * return the best-matching chord from [candidateChords] using a log-frequency-weighted chroma
     * vector. A result is only returned when the same chord is detected consistently across a
     * [WINDOW_SIZE]-frame sliding window.
     *
     * Pass an empty list to indicate silence; this advances the window with a null entry so that
     * silence correctly resets the consistency requirement.
     */
    fun recognize(detectedBins: List<FrequencyBin>): RecognitionResult? {
        if (candidateChords.isEmpty()) return null

        if (detectedBins.isEmpty()) {
            pushWindow(null)
            return null
        }

        // Build log-frequency-weighted 12-bin chroma vector.
        // log(f / MIN_HZ + 1) gives higher-frequency notes proportionally more weight,
        // compensating for the natural loudness dominance of low-frequency fundamentals.
        val chroma = DoubleArray(12)
        for (bin in detectedBins) {
            if (bin.frequencyHz < NoteFrequencyTable.MIN_INSTRUMENT_HZ) continue
            val note = NoteFrequencyTable.hzToNote(bin.frequencyHz)
            val logWeight = ln(bin.frequencyHz / NoteFrequencyTable.MIN_INSTRUMENT_HZ + 1.0)
            chroma[note.semitone] += bin.magnitude * logWeight
        }
        val maxChroma = chroma.maxOrNull() ?: 0.0
        if (maxChroma <= 0.0) {
            pushWindow(null)
            return null
        }
        for (i in chroma.indices) chroma[i] /= maxChroma

        // Detected note set: chroma bins whose normalized energy exceeds the threshold
        val detectedNotes = Note.entries
            .filter { chroma[it.semitone] >= CHROMA_THRESHOLD }
            .toSet()

        // Early termination: find the best match with early termination
        var bestChord: ChordDefinition? = null
        var bestScore = 0f
        for (chord in candidateChords) {
            val score = computeScore(chroma, chord)
            // Early termination: if we've already found a very high score, stop early
            if (score >= MIN_CONFIDENCE && score > bestScore) {
                bestScore = score
                bestChord = chord
                // Early termination: if we have a perfect match, no need to continue
                if (bestScore >= 0.95f) break
            } else if (score > bestScore) {
                bestScore = score
                bestChord = chord
            }
        }

        val matchId = if (bestScore >= MIN_CONFIDENCE) bestChord?.id else null
        pushWindow(matchId)

        // Require consistent detection across all [WINDOW_SIZE] frames
        if (recentChordIds.size < WINDOW_SIZE) return null
        val consistentId = recentChordIds.first()
        if (consistentId == null || recentChordIds.any { it != consistentId }) return null

        val matchedChord = candidateChords.find { it.id == consistentId } ?: return null
        return RecognitionResult(
            chord = matchedChord,
            detectedNotes = detectedNotes,
            confidence = bestScore
        )
    }

    /**
     * Score a candidate chord against the log-weighted chroma vector.
     *
     * - Coverage: mean chroma energy at each of the chord's note components.
     * - Extra-note penalty: energy in non-chord semitones (reduces false positives).
     * - Fingering bonus: reward if the top chroma notes align with the chord's actual
     *   guitar fingering voicing (strict template matching against [NoteFrequencyTable.GUITAR_OPEN_STRING_MIDIS]).
     *
     * Early termination optimization: if the score is already low, we can early terminate.
     */
    private fun computeScore(chroma: DoubleArray, chord: ChordDefinition): Float {
        val target = getChordComponents(chord)
        if (target.isEmpty()) return 0f

        // Early termination: if target is empty, no need to compute further
        if (target.isEmpty()) return 0f

        // Early termination: compute a quick coverage estimate to early terminate
        val coverage = target.sumOf { chroma[it.semitone] } / target.size
        if (coverage < 0.05) return 0f // Early termination for very low coverage

        val targetSemitones = target.map { it.semitone }.toSet()
        val extraEnergy = chroma.indices
            .filter { it !in targetSemitones }
            .sumOf { chroma[it] }
        val extraPenalty = (extraEnergy / 12.0 * 0.4).coerceAtMost(0.25)

        val fingeringBonus = if (matchesFingeringTemplate(chroma, chord)) 0.1 else 0.0

        val score = (coverage - extraPenalty + fingeringBonus).toFloat().coerceIn(0f, 1f)

        // Early termination: if score is too low to be considered valid, return early
        if (score < 0.1f) return 0f

        return score
    }

    /**
     * Strict template check: verify that the top-energy chroma notes overlap with
     * the actual notes produced by at least one of the chord's fingering voicings,
     * computed from standard guitar tuning ([NoteFrequencyTable.GUITAR_OPEN_STRING_MIDIS]).
     *
     * Optimized to early terminate when a match is found
     */
    private fun matchesFingeringTemplate(chroma: DoubleArray, chord: ChordDefinition): Boolean {
        val openStringMidis = NoteFrequencyTable.GUITAR_OPEN_STRING_MIDIS
        val topNotes = Note.entries
            .sortedByDescending { chroma[it.semitone] }
            .take(3)
            .toSet()

        // Early termination: check only the first few fingerings that might match
        val fingeringsToCheck = chord.fingerings.take(3) // Limit to first 3 fingerings for performance
        for (fingering in fingeringsToCheck) {
            val fingeringNotes = fingering.positions
                .filter { it.fret >= 0 && it.stringIndex < openStringMidis.size }
                .map { pos -> Note.fromSemitone(openStringMidis[pos.stringIndex] + pos.fret) }
                .toSet()
            if (fingeringNotes.isEmpty()) continue
            if (topNotes.intersect(fingeringNotes).size >= 2) return true
        }
        return false
    }

    private fun pushWindow(id: String?) {
        recentChordIds.addLast(id)
        if (recentChordIds.size > WINDOW_SIZE) recentChordIds.removeFirst()
    }
}
