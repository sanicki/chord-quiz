package com.chordquiz.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Radix-2 Cooley-Tukey FFT implementation.
 * Input must be a power-of-2 length.
 */
object FftAnalyzer {

    /**
     * Compute the magnitude spectrum of [samples].
     * Applies a Hann window before the FFT.
     *
     * @param samples   PCM short samples (will be normalized to [-1.0, 1.0])
     * @param sampleRate sample rate in Hz (e.g. 44100)
     * @return [FrequencyBin] list, only bins up to [NoteFrequencyTable.MAX_INSTRUMENT_HZ]
     */
    fun analyze(samples: ShortArray, sampleRate: Int = 44100): List<FrequencyBin> {
        val n = nextPowerOfTwo(samples.size.coerceAtMost(4096))
        val real = DoubleArray(n)
        val imag = DoubleArray(n)

        // Fill and apply Hann window
        for (i in 0 until n) {
            val sample = if (i < samples.size) samples[i].toDouble() / Short.MAX_VALUE else 0.0
            val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            real[i] = sample * hann
        }

        fft(real, imag)

        val freqResolution = sampleRate.toDouble() / n
        val maxBin = (NoteFrequencyTable.MAX_INSTRUMENT_HZ / freqResolution).toInt()
            .coerceAtMost(n / 2)
        val minBin = (NoteFrequencyTable.MIN_INSTRUMENT_HZ / freqResolution).toInt()
            .coerceAtLeast(1)

        return (minBin..maxBin).map { bin ->
            val magnitude = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin])
            FrequencyBin(bin * freqResolution, magnitude)
        }
    }

    /**
     * Find spectral peaks above [threshold] × max magnitude.
     * Returns up to [maxPeaks] peaks, sorted by magnitude descending.
     */
    fun findPeaks(
        bins: List<FrequencyBin>,
        threshold: Double = 0.1,
        maxPeaks: Int = 6
    ): List<FrequencyBin> {
        if (bins.isEmpty()) return emptyList()
        val maxMag = bins.maxOf { it.magnitude }
        val minMag = maxMag * threshold
        return bins
            .windowed(3) { window ->
                val (prev, cur, next) = window
                if (cur.magnitude > prev.magnitude && cur.magnitude > next.magnitude && cur.magnitude >= minMag)
                    cur else null
            }
            .filterNotNull()
            .sortedByDescending { it.magnitude }
            .take(maxPeaks)
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var result = 1
        while (result < n) result = result shl 1
        return result
    }

    /** In-place Cooley-Tukey FFT */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }
        // Butterfly operations
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wReal = cos(angle)
            val wImag = sin(angle)
            var i = 0
            while (i < n) {
                var wrR = 1.0; var wrI = 0.0
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * wrR - imag[i + k + len / 2] * wrI
                    val vI = real[i + k + len / 2] * wrI + imag[i + k + len / 2] * wrR
                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR
                    imag[i + k + len / 2] = uI - vI
                    val newWrR = wrR * wReal - wrI * wImag
                    wrI = wrR * wImag + wrI * wReal
                    wrR = newWrR
                }
                i += len
            }
            len = len shl 1
        }
    }
}

data class FrequencyBin(val frequencyHz: Double, val magnitude: Double)
