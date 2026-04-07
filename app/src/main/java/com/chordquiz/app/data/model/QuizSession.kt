package com.chordquiz.app.data.model

enum class QuizMode { DRAW, PLAY, NOTE_DRAW, NOTE_PLAY }

sealed class QuizQuestion {
    data class ChordQuestion(
        val chordDefinition: ChordDefinition,
        val targetFingeringIndex: Int = 0
    ) : QuizQuestion()

    data class NoteQuestion(
        val note: Note,
        val octave: Int,
        val displayName: String,
        val noteMode: NoteMode
    ) : QuizQuestion()
}

data class QuizAnswer(
    val question: QuizQuestion,
    val isCorrect: Boolean,
    val userFingering: Fingering? = null,
    val detectedNotes: List<Note> = emptyList(),
    val detectedNote: Note? = null
)

data class QuizSession(
    val id: String,
    val mode: QuizMode,
    val instrument: Instrument,
    val questions: List<QuizQuestion>,
    val answers: List<QuizAnswer> = emptyList(),
    val repeatMissed: Boolean = true,
    val startedAt: Long = System.currentTimeMillis()
) {
    val currentIndex: Int get() = answers.size
    val isComplete: Boolean get() = currentIndex >= questions.size
    val score: Int get() = answers.count { it.isCorrect }
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
}
