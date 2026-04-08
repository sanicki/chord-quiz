package com.chordquiz.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Precision metronome engine for Strum Practice mode.
 *
 * Uses [SoundPool] for zero-latency audio clicks (High = beat 1, Low = all
 * other beats) and a coroutine loop anchored to [System.nanoTime] for drift-
 * free timing.  BPM and speed percentage can be updated at any time without
 * stopping the loop — the new values take effect on the very next tick.
 *
 * Interval formula (per issue spec): `60 000 / (effectiveBpm × subdivisions)`
 * where `effectiveBpm = targetBpm × speedPercent / 100`.
 */
@Singleton
class StrumMetronomeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── Tick events emitted to the ViewModel ───────────────────────────────
    private val _tickFlow = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val tickFlow: SharedFlow<Int> = _tickFlow.asSharedFlow()

    // ── Mutable runtime parameters (read by the timing loop each tick) ─────
    @Volatile private var bpm: Int = 90
    @Volatile private var speedPercent: Int = 100
    @Volatile private var subdivisions: Double = 1.0  // clicks per beat
    @Volatile private var slotCount: Int = 4
    @Volatile private var resetRequested: Boolean = false

    // ── SoundPool ──────────────────────────────────────────────────────────
    private var soundPool: SoundPool? = null
    private var highSoundId: Int = 0
    private var lowSoundId: Int = 0
    private var soundsLoaded: Int = 0  // incremented by OnLoadCompleteListener

    // ── Coroutine infra ────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) { initSoundPool() }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    fun start() {
        loopJob?.cancel()
        resetRequested = true
        loopJob = scope.launch {
            // Wait briefly for SoundPool to finish loading (max ~1 s)
            var waited = 0
            while (soundsLoaded < 2 && waited < 20) {
                delay(50)
                waited++
            }
            runLoop()
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    /** Update target BPM (40–240). Takes effect on the next tick. */
    fun updateBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(40, 240)
    }

    /** Update speed percentage (1–100 %). Takes effect on the next tick. */
    fun updateSpeedPercent(pct: Int) {
        speedPercent = pct.coerceIn(1, 100)
    }

    /**
     * Change the note subdivision / slot count. Resets the playhead to slot 0
     * on the next tick so the grid stays in sync.
     */
    fun updateNoteType(slotCount: Int, subdivisionsPerBeat: Double) {
        this.slotCount = slotCount
        this.subdivisions = subdivisionsPerBeat
        resetRequested = true
    }

    // ── Timing loop ────────────────────────────────────────────────────────

    private suspend fun runLoop() {
        var currentSlot = 0
        var nextTickNs = System.nanoTime()

        while (currentCoroutineContext().isActive) {
            // Handle a pending reset (note-type change or fresh start)
            if (resetRequested) {
                resetRequested = false
                currentSlot = 0
                nextTickNs = System.nanoTime()
            }

            // Fire click and emit playhead position
            val isAccent = currentSlot == 0
            soundPool?.play(
                if (isAccent) highSoundId else lowSoundId,
                1f, 1f, 1, 0, 1f
            )
            _tickFlow.tryEmit(currentSlot)

            // Advance playhead
            currentSlot = (currentSlot + 1) % slotCount

            // Compute interval with the *current* (possibly just-updated) params
            val effectiveBpm = bpm * speedPercent / 100.0
            val intervalMs = (60_000.0 / (effectiveBpm * subdivisions))
                .toLong().coerceAtLeast(10L)

            nextTickNs += intervalMs * 1_000_000L
            val sleepMs = (nextTickNs - System.nanoTime()) / 1_000_000L
            if (sleepMs > 0) delay(sleepMs)
        }
    }

    // ── SoundPool setup ────────────────────────────────────────────────────

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        pool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundsLoaded++
        }

        val highFile = File(context.cacheDir, "strum_click_high.wav")
        val lowFile  = File(context.cacheDir, "strum_click_low.wav")

        writeClickWav(highFile, frequencyHz = 1200, durationMs = 20)
        writeClickWav(lowFile,  frequencyHz = 800,  durationMs = 20)

        highSoundId = pool.load(highFile.absolutePath, 1)
        lowSoundId  = pool.load(lowFile.absolutePath,  1)
        soundPool   = pool
    }

    /**
     * Writes a percussive sine-burst WAV to [file].
     * The sound has a near-instantaneous attack and exponential decay so it
     * feels like a wood-block click rather than a sustained tone.
     */
    private fun writeClickWav(file: File, frequencyHz: Int, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val dataSize   = numSamples * 2  // 16-bit PCM
        val buf        = ByteArray(44 + dataSize)

        fun putInt(off: Int, v: Int) {
            buf[off]     = (v        and 0xFF).toByte()
            buf[off + 1] = (v shr 8  and 0xFF).toByte()
            buf[off + 2] = (v shr 16 and 0xFF).toByte()
            buf[off + 3] = (v shr 24 and 0xFF).toByte()
        }
        fun putShort(off: Int, v: Int) {
            buf[off]     = (v       and 0xFF).toByte()
            buf[off + 1] = (v shr 8 and 0xFF).toByte()
        }

        // RIFF chunk
        buf[0]  = 'R'.code.toByte(); buf[1]  = 'I'.code.toByte()
        buf[2]  = 'F'.code.toByte(); buf[3]  = 'F'.code.toByte()
        putInt(4,  36 + dataSize)
        buf[8]  = 'W'.code.toByte(); buf[9]  = 'A'.code.toByte()
        buf[10] = 'V'.code.toByte(); buf[11] = 'E'.code.toByte()
        // fmt sub-chunk
        buf[12] = 'f'.code.toByte(); buf[13] = 'm'.code.toByte()
        buf[14] = 't'.code.toByte(); buf[15] = ' '.code.toByte()
        putInt(16, 16)           // PCM chunk size
        putShort(20, 1)          // PCM format
        putShort(22, 1)          // mono
        putInt(24, sampleRate)
        putInt(28, sampleRate * 2)
        putShort(32, 2)          // block align
        putShort(34, 16)         // bits per sample
        // data sub-chunk
        buf[36] = 'd'.code.toByte(); buf[37] = 'a'.code.toByte()
        buf[38] = 't'.code.toByte(); buf[39] = 'a'.code.toByte()
        putInt(40, dataSize)

        // PCM: sine wave with exponential decay envelope
        for (i in 0 until numSamples) {
            val t       = i.toDouble() / sampleRate
            val decay   = exp(-t * 120.0)          // fast percussive tail
            val sample  = (Short.MAX_VALUE * 0.85 * sin(2.0 * PI * frequencyHz * t) * decay)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            putShort(44 + i * 2, sample)
        }

        file.writeBytes(buf)
    }
}
