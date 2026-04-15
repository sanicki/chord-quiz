package com.chordquiz.app.domain.model

enum class Difficulty(
    /** Minimum chord recognition confidence score (0–1) required to accept a detected chord. */
    val acceptanceThreshold: Float,
    val requiresRoot: Boolean,
    val requiresThird: Boolean,
    val windowSize: Int,
    val extraPenaltyFactor: Double
) {
    /** Forgiving: accepts rough chord shapes, no specific note requirements. */
    EASY(acceptanceThreshold = 0.35f, requiresRoot = false, requiresThird = false, windowSize = 2, extraPenaltyFactor = 0.15),
    /** Standard: requires root note to be present. */
    MEDIUM(acceptanceThreshold = 0.65f, requiresRoot = true, requiresThird = false, windowSize = 3, extraPenaltyFactor = 0.30),
    /** Strict: requires root and third, high confidence. */
    HARD(acceptanceThreshold = 0.80f, requiresRoot = true, requiresThird = true, windowSize = 4, extraPenaltyFactor = 0.40);

    companion object {
        val DEFAULT = EASY
    }
}
