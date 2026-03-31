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

    /** Play the note at MIDI number [midi] for [durationMs] milliseconds. */
    suspend fun playMidi(midi: Int, durationMs: Int = 300) {
        playHz(NoteFrequencyTable.midiToHz(midi), durationMs)
    }

    private suspend fun playHz(hz: Double, durationMs: Int) = withContext(Dispatchers.IO) {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val buffer = ShortArray(sampleCount)
        val angularFreq = 2.0 * PI * hz / SAMPLE_RATE
        for (i in buffer.indices) {
            val t = i.toDouble() / sampleCount
            val envelope = when {
                t < 0.05 -> t / 0.05
                t > 0.75 -> (1.0 - t) / 0.25
                else -> 1.0
            }
            buffer[i] = (sin(angularFreq * i) * Short.MAX_VALUE * 0.4 * envelope).toInt().toShort()
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
