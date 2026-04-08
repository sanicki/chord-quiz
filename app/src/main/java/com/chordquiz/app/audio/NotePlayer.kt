package com.chordquiz.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Plays notes and chords using Karplus-Strong physical modelling synthesis.
 * This produces a natural plucked-string timbre that decays exponentially,
 * closely approximating real guitar, ukulele, bass and banjo tones.
 *
 * Each instrument is characterised by three parameters:
 *  - decay:      feedback gain per sample cycle (higher → longer sustain)
 *  - filterCoeff: low-pass blend weight (0 = bright, 1 = dull)
 *  - maxDurationMs: hard cap on playback so banjo doesn't ring forever
 */
object NotePlayer {

    private const val SAMPLE_RATE = 44100

    private data class InstrumentConfig(
        val decay: Float,
        val filterCoeff: Float,
        val maxDurationMs: Int
    )

    private val CONFIGS = mapOf(
        // Acoustic guitar: warm, moderate sustain
        "guitar_standard" to InstrumentConfig(decay = 0.9964f, filterCoeff = 0.50f, maxDurationMs = 2000),
        // Soprano ukulele: bright and slightly shorter sustain than guitar
        "ukulele_soprano"  to InstrumentConfig(decay = 0.9935f, filterCoeff = 0.45f, maxDurationMs = 1500),
        // Acoustic bass: deep, long sustain, slightly more filtered
        "bass_standard"    to InstrumentConfig(decay = 0.9981f, filterCoeff = 0.55f, maxDurationMs = 3000),
        // Banjo: bright, metallic, fast decay
        "banjo_5string"    to InstrumentConfig(decay = 0.9880f, filterCoeff = 0.40f, maxDurationMs = 900),
    )
    private val DEFAULT_CONFIG = InstrumentConfig(decay = 0.9950f, filterCoeff = 0.50f, maxDurationMs = 1500)

    /** Play a single note (MIDI pitch) with the timbre of [instrumentId]. */
    suspend fun playNote(midi: Int, instrumentId: String, durationMs: Int = 500) {
        val cfg = CONFIGS[instrumentId] ?: DEFAULT_CONFIG
        playKS(
            frequencies = listOf(NoteFrequencyTable.midiToHz(midi)),
            config = cfg,
            durationMs = minOf(durationMs, cfg.maxDurationMs)
        )
    }

    /** Play a chord (list of MIDI pitches) with the timbre of [instrumentId]. */
    suspend fun playChord(midis: List<Int>, instrumentId: String, durationMs: Int = 1500) {
        if (midis.isEmpty()) return
        val cfg = CONFIGS[instrumentId] ?: DEFAULT_CONFIG
        playKS(
            frequencies = midis.map { NoteFrequencyTable.midiToHz(it) },
            config = cfg,
            durationMs = minOf(durationMs, cfg.maxDurationMs)
        )
    }

    /**
     * Karplus-Strong synthesis core.
     *
     * For each frequency:
     *  1. Initialise a circular delay line with white noise (the "pluck" excitation).
     *  2. On each sample tick, output the current value then replace it with a
     *     weighted average of the current and next samples multiplied by [decay].
     *     This one-pole low-pass filter + feedback loop produces the characteristic
     *     exponentially-decaying pitched tone.
     *
     * Multiple frequencies are computed simultaneously and averaged for chord output.
     */
    private suspend fun playKS(
        frequencies: List<Double>,
        config: InstrumentConfig,
        durationMs: Int
    ) = withContext(Dispatchers.IO) {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val outputBuffer = ShortArray(sampleCount)

        // Use a deterministic seed so the timbre is consistent across calls
        val rng = java.util.Random(0xC0FFEEL)

        // Initialise one delay line per frequency
        val delayLines = frequencies.map { hz ->
            val size = (SAMPLE_RATE / hz).toInt().coerceAtLeast(2)
            FloatArray(size) { rng.nextFloat() * 2f - 1f }
        }
        val readPos = IntArray(frequencies.size)

        for (i in 0 until sampleCount) {
            var mixed = 0f
            for (j in frequencies.indices) {
                val dl = delayLines[j]
                val pos = readPos[j]
                val nextPos = (pos + 1) % dl.size

                val out = dl[pos]
                // One-pole low-pass filter + feedback (Karplus-Strong recurrence)
                dl[pos] = (dl[pos] * (1f - config.filterCoeff) +
                           dl[nextPos] * config.filterCoeff) * config.decay
                readPos[j] = nextPos
                mixed += out
            }

            val sample = (mixed / frequencies.size) * Short.MAX_VALUE * 0.80f
            outputBuffer[i] = sample.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, sampleCount * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(outputBuffer, 0, sampleCount)
        track.play()
        delay(durationMs.toLong())
        track.stop()
        track.release()
    }
}
