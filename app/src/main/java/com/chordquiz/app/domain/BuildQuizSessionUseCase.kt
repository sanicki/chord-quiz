package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Instrument
import com.chordquiz.app.data.model.QuizMode
import com.chordquiz.app.data.model.QuizQuestion
import com.chordquiz.app.data.model.QuizSession
import com.chordquiz.app.data.model.ChordDefinition
import java.util.UUID
import javax.inject.Inject

class BuildQuizSessionUseCase @Inject constructor() {
    operator fun invoke(
        instrument: Instrument,
        mode: QuizMode,
        selectedChords: List<ChordDefinition>,
        questionCount: Int,
        repeatMissed: Boolean
    ): QuizSession {
        val questions = buildQuestionList(selectedChords, questionCount)
            .map { QuizQuestion.ChordQuestion(it) }
        return QuizSession(
            id = UUID.randomUUID().toString(),
            mode = mode,
            instrument = instrument,
            questions = questions,
            repeatMissed = repeatMissed
        )
    }
}
