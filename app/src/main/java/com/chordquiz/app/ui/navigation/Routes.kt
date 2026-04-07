package com.chordquiz.app.ui.navigation

import com.chordquiz.app.data.model.NoteMode
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

@Serializable
data class NoteQuizModeRoute(val instrumentId: String)

@Serializable
data class NotePracticeSetupRoute(
    val instrumentId: String,
    val noteMode: NoteMode,
    val preserveSettings: Boolean = false,
    val initialRepeatMissed: Boolean = true
)

@Serializable
data class NoteDrawQuizRoute(
    val instrumentId: String,
    val noteMode: NoteMode,
    val questionCount: Int,
    val repeatMissed: Boolean
)

@Serializable
data class NotePlayQuizRoute(
    val instrumentId: String,
    val noteMode: NoteMode,
    val questionCount: Int,
    val repeatMissed: Boolean
)
