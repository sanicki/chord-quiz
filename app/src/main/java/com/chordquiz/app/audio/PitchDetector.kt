package com.chordquiz.app.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Polyphonic pitch detection combining FFT peak-picking and YIN-like
 * harmonic product spectrum for fundamental frequency estimation.
 */
object PitchDetector {

    /**
     * Returns a list of detected fundamental frequencies (Hz) from [samples].
     * Uses spectral peak-picking with harmonic suppression to find multiple pitches.
     *
     * @param samples PCM short samples
     * @param sampleRate sample rate in Hz
     * @param maxPitches maximum number of simultaneous pitches to detect
     */
    fun detectPitches(
        samples: ShortArray,
        sampleRate: Int = 44100,
        maxPitches: Int = 6
    ): List<Double> {
        val bins = FftAnalyzer.analyze(samples, sampleRate)
        if (bins.isEmpty()) return emptyList()

        val peaks = FftAnalyzer.findPeaks(bins, threshold = 0.08, maxPeaks = 20)
        if (peaks.isEmpty()) return emptyList()

        // Filter to instrument frequency range
        val fundamentals = mutableListOf<Double>()
        val suppressedFreqs = mutableSetOf<Double>()

        for (peak in peaks.sortedByDescending { it.magnitude }) {
            val freq = peak.frequencyHz
            if (freq < NoteFrequencyTable.MIN_INSTRUMENT_HZ) continue

            // Check if this frequency is already a harmonic of a detected fundamental
            val isHarmonic = fundamentals.any { f ->
                val ratio = freq / f
                val nearestHarmonic = ratio.toInt().coerceAtLeast(1)
                abs(ratio - nearestHarmonic) < 0.05 * nearestHarmonic
            }

            if (!isHarmonic) {
                fundamentals.add(freq)
                if (fundamentals.size >= maxPitches) break
            }
        }

        return fundamentals
    }

    /** Compute overall signal amplitude (RMS) normalized 0.0–1.0 */
    fun computeAmplitude(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sumSq = 0.0
        for (s in samples) sumSq += (s.toDouble() / Short.MAX_VALUE).let { it * it }
        return sqrt(sumSq / samples.size).toFloat().coerceIn(0f, 1f)
    }
}
