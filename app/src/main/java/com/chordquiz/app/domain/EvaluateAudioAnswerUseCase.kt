package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.domain.model.Difficulty
import javax.inject.Inject

class EvaluateAudioAnswerUseCase @Inject constructor() {

    /**
     * Minimum normalized RMS amplitude required for audio to be processed.
     * Initialized to a safe default; updated by [calibrateNoise] at recorder start.
     */
    var dynamicSilenceThreshold: Float = 0.02f
        private set

    /**
     * Measure the ambient noise level at recorder start and set the silence gate
     * to 2× that level so that only genuine playing is evaluated.
     *
     * @param ambientRms normalized RMS amplitude (0.0–1.0) measured over quiet frames
     */
    fun calibrateNoise(ambientRms: Float) {
        dynamicSilenceThreshold = (ambientRms * 2f).coerceAtLeast(0.01f).coerceAtMost(0.15f)
    }

    /**
     * Returns true if the detected notes match the target chord given the
     * selected [difficulty]. Higher difficulties raise the coverage threshold
     * and enforce that specific chord tones (root, third) are present.
     */
    operator fun invoke(
        detectedNotes: Set<Note>,
        target: ChordDefinition,
        difficulty: Difficulty = Difficulty.DEFAULT
    ): Boolean {
        if (detectedNotes.isEmpty()) return false
        val targetSet = target.noteComponents.toSet()
        val intersection = detectedNotes.intersect(targetSet)
        val score = intersection.size.toFloat() / targetSet.size.toFloat()
        if (score < difficulty.acceptanceThreshold) return false
        if (difficulty.requiresRoot && target.rootNote !in detectedNotes) return false
        if (difficulty.requiresThird) {
            val thirdInterval = target.chordType.intervals.getOrElse(1) { 4 }
            val third = target.rootNote.plus(thirdInterval)
            if (third !in detectedNotes) return false
        }
        return true
    }
}
