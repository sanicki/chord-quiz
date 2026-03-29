package com.chordquiz.app.domain

import com.chordquiz.app.data.model.ChordDefinition
import com.chordquiz.app.data.model.Note
import javax.inject.Inject

class EvaluateAudioAnswerUseCase @Inject constructor() {

    /**
     * Returns true if the detected notes match the target chord with
     * at least 60% note coverage (handles missing/extra strings).
     */
    operator fun invoke(
        detectedNotes: Set<Note>,
        target: ChordDefinition
    ): Boolean {
        if (detectedNotes.isEmpty()) return false
        val targetSet = target.noteComponents.toSet()
        val intersection = detectedNotes.intersect(targetSet)
        val score = intersection.size.toFloat() / targetSet.size.toFloat()
        return score >= 0.6f
    }
}
