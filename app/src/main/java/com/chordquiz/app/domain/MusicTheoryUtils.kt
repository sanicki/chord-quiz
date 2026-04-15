package com.chordquiz.app.domain

/**
 * Maps a chromatic semitone (0 = C, 1 = C#, …, 11 = B) to its diatonic
 * staff position (0 = C, 1 = D, 2 = E, 3 = F, 4 = G, 5 = A, 6 = B).
 */
fun semitoneToDiatonic(semitone: Int): Int = when (semitone % 12) {
    0, 1  -> 0   // C, C#
    2     -> 1   // D
    3, 4  -> 2   // Eb/D#, E
    5, 6  -> 3   // F, F#
    7, 8  -> 4   // G, G#
    9, 10 -> 5   // A, Bb
    11    -> 6   // B
    else  -> 0
}
