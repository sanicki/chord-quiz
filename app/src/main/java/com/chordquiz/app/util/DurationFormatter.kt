package com.chordquiz.app.util

/**
 * Formats a duration in milliseconds as MM:SS when under one hour,
 * or H:MM:SS when one hour or more.
 */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
