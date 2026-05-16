package com.beecareanywhere.multimodal

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records 16 kHz mono 16-bit PCM audio into an in-memory buffer.
 *
 * LiteRT-LM consumes audio via `Content.AudioBytes(ByteArray)`, so [stop] returns the raw PCM
 * buffer ready to hand to [com.beecareanywhere.model.BeekeepingModel.diagnose].
 *
 * Single-use: construct, [start], [stop]. Construct a new instance for each recording.
 *
 * Caller must hold `Manifest.permission.RECORD_AUDIO` before invoking [start]; otherwise [start]
 * returns false.
 */
class AudioCapture {

    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private val buffer = ByteArrayOutputStream()

    val sampleRate: Int = SAMPLE_RATE_HZ

    /** Returns true if recording started successfully. */
    @SuppressLint("MissingPermission") // Caller checks RECORD_AUDIO; we return false on SecurityException.
    fun start(): Boolean {
        check(!isRecording.get()) { "Already recording" }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return false

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        } catch (_: SecurityException) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }

        recorder = rec
        buffer.reset()
        isRecording.set(true)
        rec.startRecording()

        thread = Thread {
            val chunk = ByteArray(minBuf)
            while (isRecording.get()) {
                val read = rec.read(chunk, 0, chunk.size)
                if (read > 0) {
                    synchronized(buffer) { buffer.write(chunk, 0, read) }
                }
            }
        }.apply { name = "BeeCare-AudioCapture"; start() }
        return true
    }

    /** Stop recording and return the captured PCM bytes. Safe to call multiple times. */
    fun stop(): ByteArray {
        if (!isRecording.compareAndSet(true, false)) {
            return synchronized(buffer) { buffer.toByteArray() }
        }
        thread?.join(STOP_TIMEOUT_MS)
        thread = null
        recorder?.runCatching {
            stop()
            release()
        }
        recorder = null
        return synchronized(buffer) { buffer.toByteArray() }
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val STOP_TIMEOUT_MS = 500L
    }
}
