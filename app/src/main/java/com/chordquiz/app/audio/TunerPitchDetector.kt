package com.chordquiz.app.audio

/**
 * Monophonic pitch detector using the YIN algorithm (normalized difference function).
 * Returns a single fundamental frequency in Hz, or null if no clear pitch is detected.
 *
 * Optimized for tuner use cases requiring cents-level accuracy on single-note input,
 * as opposed to the polyphonic FFT-based PitchDetector used for chord recognition.
 */
object TunerPitchDetector {

    private const val SILENCE_THRESHOLD = 0.02f
    private const val YIN_THRESHOLD = 0.15f
    private const val YIN_FALLBACK_THRESHOLD = 0.35

    // Frequency range covering all instrument open strings with margin
    private const val MIN_FREQ_HZ = 20.0
    private const val MAX_FREQ_HZ = 1200.0

    /**
     * Detect the fundamental frequency of [samples] using the YIN algorithm.
     * Returns null if the signal is too quiet or no clear fundamental is found.
     *
     * @param samples   PCM short samples from the microphone
     * @param sampleRate sample rate in Hz (defaults to AudioRecorderManager.SAMPLE_RATE)
     */
    fun detectPitch(
        samples: ShortArray,
        sampleRate: Int = AudioRecorderManager.SAMPLE_RATE
    ): Float? {
        if (PitchDetector.computeAmplitude(samples) < SILENCE_THRESHOLD) return null

        // Clamp buffer to 4096 samples for consistent performance
        val n = minOf(samples.size, 4096)
        val buf = DoubleArray(n) { samples[it].toDouble() / Short.MAX_VALUE }

        val minPeriod = (sampleRate / MAX_FREQ_HZ).toInt().coerceAtLeast(2)
        val maxPeriod = (sampleRate / MIN_FREQ_HZ).toInt().coerceAtMost(n / 2)

        if (minPeriod >= maxPeriod) return null

        // Step 1 & 2: compute cumulative mean normalized difference function (CMNDF)
        val ndf = DoubleArray(maxPeriod + 1)
        ndf[0] = 1.0
        var runningSum = 0.0

        for (tau in 1..maxPeriod) {
            var diff = 0.0
            val limit = n - tau
            for (j in 0 until limit) {
                val d = buf[j] - buf[j + tau]
                diff += d * d
            }
            runningSum += diff
            ndf[tau] = if (runningSum > 0.0) diff * tau / runningSum else 1.0
        }

        // Step 3: find first tau where NDF drops below threshold, starting at minPeriod.
        // Walk to the local minimum of that valley for best accuracy.
        var bestTau = -1
        var tau = minPeriod
        while (tau <= maxPeriod) {
            if (ndf[tau] < YIN_THRESHOLD) {
                bestTau = tau
                while (bestTau + 1 <= maxPeriod && ndf[bestTau + 1] < ndf[bestTau]) {
                    bestTau++
                }
                break
            }
            tau++
        }

        // Fallback: use global minimum if no threshold crossing found
        if (bestTau == -1) {
            var minVal = Double.MAX_VALUE
            for (t in minPeriod..maxPeriod) {
                if (ndf[t] < minVal) {
                    minVal = ndf[t]
                    bestTau = t
                }
            }
            if (minVal > YIN_FALLBACK_THRESHOLD) return null
        }

        if (bestTau <= 0) return null

        // Step 4: parabolic interpolation for sub-sample accuracy
        val refinedTau: Double = if (bestTau in 1 until maxPeriod) {
            val s0 = ndf[bestTau - 1]
            val s1 = ndf[bestTau]
            val s2 = ndf[bestTau + 1]
            val denom = s0 - 2.0 * s1 + s2
            if (denom != 0.0) bestTau + (s0 - s2) / (2.0 * denom) else bestTau.toDouble()
        } else {
            bestTau.toDouble()
        }

        return if (refinedTau > 0.0) (sampleRate / refinedTau).toFloat() else null
    }
}
