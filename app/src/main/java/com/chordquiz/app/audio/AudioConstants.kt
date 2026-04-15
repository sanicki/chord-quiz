package com.chordquiz.app.audio

/**
 * Centralized audio processing thresholds and constants.
 * Single source of truth for tuning values used across chord recognition and pitch detection.
 */
object AudioConstants {

    /** RMS amplitude below which audio is treated as silence. */
    const val SILENCE_THRESHOLD = 0.02f

    /** Minimum chroma vector similarity to accept a chord match. */
    const val CHROMA_THRESHOLD = 0.15
}
