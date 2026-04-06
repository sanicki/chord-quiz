package com.chordquiz.app.domain.model

enum class Difficulty(
    val acceptanceThreshold: Float,
    val requiresRoot: Boolean,
    val requiresThird: Boolean
) {
    EASY(acceptanceThreshold = 0.50f, requiresRoot = false, requiresThird = false),
    MEDIUM(acceptanceThreshold = 0.65f, requiresRoot = true, requiresThird = false),
    HARD(acceptanceThreshold = 0.80f, requiresRoot = true, requiresThird = true);

    companion object {
        val DEFAULT = EASY
    }
}
