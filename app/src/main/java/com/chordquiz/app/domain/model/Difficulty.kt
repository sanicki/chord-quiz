package com.chordquiz.app.domain.model

enum class Difficulty(
    val acceptanceThreshold: Float,
    val requiresRoot: Boolean,
    val requiresThird: Boolean,
    val windowSize: Int,
    val extraPenaltyFactor: Double
) {
    EASY(acceptanceThreshold = 0.50f, requiresRoot = false, requiresThird = false, windowSize = 2, extraPenaltyFactor = 0.15),
    MEDIUM(acceptanceThreshold = 0.65f, requiresRoot = true, requiresThird = false, windowSize = 3, extraPenaltyFactor = 0.30),
    HARD(acceptanceThreshold = 0.80f, requiresRoot = true, requiresThird = true, windowSize = 4, extraPenaltyFactor = 0.40);

    companion object {
        val DEFAULT = EASY
    }
}
