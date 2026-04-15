package com.chordquiz.app.domain

/**
 * Returns exactly [count] items drawn from [candidates] by shuffling and
 * repeating the list as many times as needed.
 */
fun <T> buildQuestionList(candidates: List<T>, count: Int): List<T> {
    require(candidates.isNotEmpty()) { "candidates must not be empty" }
    val shuffled = candidates.shuffled()
    return generateSequence { shuffled }
        .flatten()
        .take(count)
        .toList()
}
