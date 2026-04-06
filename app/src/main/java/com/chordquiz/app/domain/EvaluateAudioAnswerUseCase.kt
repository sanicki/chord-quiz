package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Note
import com.chordquiz.app.domain.model.Difficulty
import javax.inject.Inject

class EvaluateAudioAnswerUseCase @Inject constructor() {

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
