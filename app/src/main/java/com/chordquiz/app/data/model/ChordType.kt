package com.chordquiz.app.data.model

enum class ChordType(val suffix: String, val displayName: String, val intervals: List<Int>) {
    MAJOR("", "Major", listOf(0, 4, 7)),
    MINOR("m", "Minor", listOf(0, 3, 7)),
    DOMINANT_7("7", "Dominant 7", listOf(0, 4, 7, 10)),
    MAJOR_7("maj7", "Major 7", listOf(0, 4, 7, 11)),
    MINOR_7("m7", "Minor 7", listOf(0, 3, 7, 10));
}
