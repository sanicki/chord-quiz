package com.chordquiz.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object InstrumentSelectionRoute

@Serializable
data class ChordLibraryRoute(val instrumentId: String)

@Serializable
data class PracticeSetupRoute(
    val instrumentId: String,
    val selectedChordIds: List<String>,
    val preserveSettings: Boolean = false,
    val initialMode: String = "DRAW",
    val initialRepeatMissed: Boolean = true
)

@Serializable
data class DrawQuizRoute(
    val instrumentId: String,
    val selectedChordIds: List<String>,
    val questionCount: Int,
    val repeatMissed: Boolean
)

@Serializable
data class PlayQuizRoute(
    val instrumentId: String,
    val selectedChordIds: List<String>,
    val questionCount: Int,
    val repeatMissed: Boolean
)

@Serializable
data class ChordPreviewRoute(
    val instrumentId: String,
    val selectedChordIds: List<String>,
    val questionCount: Int,
    val repeatMissed: Boolean,
    val quizMode: String // "draw" | "play"
)

@Serializable
data class ResultsRoute(val sessionId: String, val restartRoute: String? = null)

@Serializable
object SettingsRoute
