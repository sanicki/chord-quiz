package com.chordquiz.app.ui.shared

import com.chordquiz.app.data.model.QuizSession

// Shared state store for passing data between screens
object SessionStore {
    val sessions = mutableMapOf<String, QuizSession>()
    fun save(session: QuizSession) { sessions[session.id] = session }
    fun get(id: String): QuizSession? = sessions[id]

    // Last quiz parameters for "Try Again" functionality
    var lastQuizType: String? = null
    var lastInstrumentId: String? = null
    var lastSelectedChordIds: List<String>? = null
    var lastQuestionCount: Int = 10
    var lastRepeatMissed: Boolean = true
    // Note quiz specific
    var lastNoteMode: String? = null
}
