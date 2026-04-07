package com.chordquiz.app.domain

import com.chordquiz.app.data.model.Note
import com.chordquiz.app.data.model.NoteMode
import com.chordquiz.app.data.model.QuizQuestion
import javax.inject.Inject

class EvaluateNoteAudioAnswerUseCase @Inject constructor() {

    operator fun invoke(
        detectedNote: Note,
        detectedOctave: Int,
        question: QuizQuestion.NoteQuestion
    ): Boolean {
        val targetSemitone = question.note.semitone
        return when (question.noteMode) {
            NoteMode.FIND_NOTE,
            NoteMode.FIND_ALL_NOTES -> detectedNote.semitone == targetSemitone

            NoteMode.FIND_NOTE_CORRECT_OCTAVE,
            NoteMode.FIND_ALL_NOTES_CORRECT_OCTAVE ->
                detectedNote.semitone == targetSemitone && detectedOctave == question.octave
        }
    }
}
