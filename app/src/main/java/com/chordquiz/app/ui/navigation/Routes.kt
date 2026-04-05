package com.chordquiz.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object InstrumentSelectionRoute

@Serializable
data class ChordLibraryRoute(val instrumentId: String)

@Serializable
data class GroupsRoute(val instrumentId: String)

@Serializable
data class PracticeSetupRoute(
    val instrumentId: String,
    val selectedChordIds: List<String>
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
data class ResultsRoute(val sessionId: String, val restartRoute: String? = null)
