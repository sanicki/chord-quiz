package com.chordquiz.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

object NotePlayer {

    private const val SAMPLE_RATE = 44100

    /**
     * Harmonic amplitude profiles per instrument.
     * Index 0 = fundamental, index 1 = 2nd harmonic, etc.
     * Values represent relative amplitude of each harmonic partial.
     */
    private val INSTRUMENT_HARMONICS = mapOf(
        // Acoustic guitar: warm, rich tone with moderate upper harmonics
        "guitar_standard" to floatArrayOf(1.0f, 0.60f, 0.30f, 0.18f, 0.08f, 0.04f),
        // Soprano ukulele: bright, thinner tone with fewer harmonics
        "ukulele_soprano"  to floatArrayOf(1.0f, 0.45f, 0.18f, 0.08f, 0.03f),
        // Acoustic bass: deep tone, dominant fundamental and 2nd harmonic
        "bass_standard"    to floatArrayOf(1.0f, 0.80f, 0.50f, 0.25f, 0.12f, 0.06f, 0.03f),
        // Banjo: bright, metallic tone with strong upper harmonics, fast decay
        "banjo_5string"    to floatArrayOf(1.0f, 0.65f, 0.55f, 0.45f, 0.35f, 0.25f, 0.18f, 0.10f)
    )
    private val DEFAULT_HARMONICS = floatArrayOf(1.0f, 0.5f, 0.25f, 0.1f)

    /** Play a single note (MIDI pitch) with the timbre of [instrumentId]. */
    suspend fun playNote(midi: Int, instrumentId: String, durationMs: Int = 300) {
        val harmonics = INSTRUMENT_HARMONICS[instrumentId] ?: DEFAULT_HARMONICS
        // Banjo has a characteristically fast decay
        val actualDuration = if (instrumentId == "banjo_5string") minOf(durationMs, 220) else durationMs
        playHz(listOf(NoteFrequencyTable.midiToHz(midi)), harmonics, actualDuration)
    }

    /**
     * Play a chord (list of MIDI pitches simultaneously) with the timbre of [instrumentId].
     * Notes are mixed together and played for [durationMs] milliseconds.
     */
    suspend fun playChord(midis: List<Int>, instrumentId: String, durationMs: Int = 1200) {
        if (midis.isEmpty()) return
        val harmonics = INSTRUMENT_HARMONICS[instrumentId] ?: DEFAULT_HARMONICS
        playHz(midis.map { NoteFrequencyTable.midiToHz(it) }, harmonics, durationMs)
    }

    private suspend fun playHz(
        frequencies: List<Double>,
        harmonics: FloatArray,
        durationMs: Int
    ) = withContext(Dispatchers.IO) {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val buffer = ShortArray(sampleCount)
        val totalGain = frequencies.size.toFloat() * harmonics.sum()

        for (i in buffer.indices) {
            val t = i.toDouble() / sampleCount
            val envelope = when {
                t < 0.02 -> t / 0.02        // fast attack (~20ms)
                t > 0.70 -> (1.0 - t) / 0.30  // gradual release (last 30%)
                else -> 1.0
            }
            var sample = 0.0
            for (hz in frequencies) {
                val angularFreq = 2.0 * PI * hz / SAMPLE_RATE
                for ((h, amp) in harmonics.withIndex()) {
                    sample += sin(angularFreq * (h + 1) * i) * amp
                }
            }
            val normalized = (sample / totalGain) * Short.MAX_VALUE * 0.75 * envelope
            buffer[i] = normalized.toInt()
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

        track.write(buffer, 0, sampleCount)
        track.play()
        delay(durationMs.toLong())
        track.stop()
        track.release()
    }
}
