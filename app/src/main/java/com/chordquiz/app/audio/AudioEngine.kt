package com.chordquiz.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/**
 * Real-time audio engine using Karplus-Strong physical modelling synthesis.
 * Produces natural plucked-string timbres that decay exponentially.
 *
 * Each instrument is characterised by:
 *  - decay: feedback gain per sample cycle (higher → longer sustain)
 *  - filterCoeff: low-pass blend weight (0 = bright, 1 = dull)
 *  - maxDurationMs: hard cap on playback
 */
class AudioEngine {

    private companion object {
        private const val SAMPLE_RATE = 44100

        private data class InstrumentConfig(
            val decay: Float,
            val filterCoeff: Float,
            val maxDurationMs: Int
        )

        private val CONFIGS = mapOf(
            "guitar_standard" to InstrumentConfig(decay = 0.9964f, filterCoeff = 0.50f, maxDurationMs = 2000),
            "ukulele_soprano" to InstrumentConfig(decay = 0.9935f, filterCoeff = 0.45f, maxDurationMs = 1500),
            "bass_standard"   to InstrumentConfig(decay = 0.9981f, filterCoeff = 0.55f, maxDurationMs = 3000),
            "banjo_5string"   to InstrumentConfig(decay = 0.9880f, filterCoeff = 0.40f, maxDurationMs = 900),
        )
        private val DEFAULT_CONFIG = InstrumentConfig(decay = 0.9950f, filterCoeff = 0.50f, maxDurationMs = 1500)
    }

    /**
     * Global monotonic clock using nanoseconds for precise timing.
     */
    private val clock: () -> Long = { System.nanoTime() }

    /**
     * Tracks active audio tracks for concurrent playback.
     */
    private val activeTracks = mutableListOf<AudioTrack>()

    /**
     * Generate Karplus-Strong audio samples for one or more frequencies.
     *
     * @param frequencies Hz frequencies to synthesize (one per note in a chord)
     * @param instrumentId Instrument preset identifier
     * @param durationMs Target duration in milliseconds
     * @return PCM audio buffer as Shorts (16-bit)
     */
    private fun generateKS(
        frequencies: List<Double>,
        config: InstrumentConfig,
        durationMs: Int
    ): ShortArray {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val outputBuffer = ShortArray(sampleCount)

        val rng = java.util.Random(0xC0FFEEL)
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
                dl[pos] = (dl[pos] * (1f - config.filterCoeff) +
                        dl[nextPos] * config.filterCoeff) * config.decay
                readPos[j] = nextPos
                mixed += out
            }

            val sample = (mixed / frequencies.size) * Short.MAX_VALUE * 0.80f
            outputBuffer[i] = sample.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return outputBuffer
    }

    /**
     * Start playback of the given audio samples using AudioTrack in static mode.
     *
     * @param samples PCM audio samples to play
     * @param durationMs Duration of the audio in milliseconds
     * @return AudioTrack instance for tracking
     */
    private fun playSamples(samples: ShortArray, durationMs: Int): AudioTrack {
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
            .setBufferSizeInBytes(maxOf(minBuf, samples.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()

        synchronized(activeTracks) {
            activeTracks += track
        }

        return track
    }

    /**
     * Stop and release a track, removing it from active tracks.
     */
    private fun stopTrack(track: AudioTrack) {
        try {
            track.stop()
        } catch (e: Exception) {
            // Ignore playback errors
        }
        track.release()

        synchronized(activeTracks) {
            activeTracks.removeAll { it == track || it.state == AudioTrack.STATE_UNINITIALIZED }
        }
    }

    /**
     * Play a single note using Karplus-Strong synthesis.
     *
     * @param midi MIDI note number (0-127)
     * @param instrumentId Instrument preset
     * @param durationMs Duration in milliseconds
     */
    suspend fun playNote(midi: Int, instrumentId: String = "guitar_standard", durationMs: Int = 500) {
        withContext(Dispatchers.IO) {
            val cfg = CONFIGS[instrumentId] ?: DEFAULT_CONFIG
            val freq = NoteFrequencyTable.midiToHz(midi)
            val cappedDuration = minOf(durationMs, cfg.maxDurationMs)
            val samples = generateKS(listOf(freq), cfg, cappedDuration)

            val track = playSamples(samples, cappedDuration)
            delay(cappedDuration.toLong())
            stopTrack(track)
        }
    }

    /**
     * Play a chord (multiple simultaneous notes).
     *
     * @param midis MIDI note numbers
     * @param instrumentId Instrument preset
     * @param durationMs Duration in milliseconds
     */
    suspend fun playChord(midis: List<Int>, instrumentId: String = "guitar_standard", durationMs: Int = 1500) {
        withContext(Dispatchers.IO) {
            if (midis.isEmpty()) return@withContext

            val cfg = CONFIGS[instrumentId] ?: DEFAULT_CONFIG
            val freqs = midis.map { NoteFrequencyTable.midiToHz(it) }
            val cappedDuration = minOf(durationMs, cfg.maxDurationMs)
            val samples = generateKS(freqs, cfg, cappedDuration)

            val track = playSamples(samples, cappedDuration)
            delay(cappedDuration.toLong())
            stopTrack(track)
        }
    }

    /**
     * Stop all currently playing audio.
     */
    suspend fun stopAll() {
        withContext(Dispatchers.Default) {
            synchronized(activeTracks) {
                activeTracks.forEach { track ->
                    try {
                        track.stop()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    track.release()
                }
                activeTracks.clear()
            }
        }
    }

    /**
     * Release all resources held by the engine.
     */
    fun release() {
        synchronized(activeTracks) {
            activeTracks.forEach { track ->
                try {
                    track.stop()
                } catch (e: Exception) {
                    // Ignore
                }
                track.release()
            }
            activeTracks.clear()
        }
    }
}
