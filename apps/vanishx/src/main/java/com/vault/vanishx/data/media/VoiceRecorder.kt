package com.vault.vanishx.data.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.vault.vanishx.domain.model.MediaLimits
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * App-private AAC-in-MPEG4 voice capture (E16-5).
 * Output MIME: [MediaLimits.VOICE_MIME] (`audio/mp4`).
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val finished = AtomicBoolean(true)
    private val mainHandler = Handler(Looper.getMainLooper())

    val isRecording: Boolean get() = !finished.get()

    fun start(onMaxDuration: () -> Unit): File {
        cancel()
        finished.set(false)
        val file = File(context.cacheDir, "voice/voice_${System.currentTimeMillis()}.m4a").apply {
            parentFile?.mkdirs()
        }
        val mr = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(AAC_BIT_RATE)
            setAudioSamplingRate(AAC_SAMPLE_RATE)
            setOutputFile(file.absolutePath)
            setMaxDuration(MediaLimits.VOICE_MAX_DURATION_MS.toInt())
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mainHandler.post { onMaxDuration() }
                }
            }
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
        return file
    }

    /** Stop and return the file, or null if too short / failed. */
    fun stop(): File? {
        if (!finished.compareAndSet(false, true)) {
            return null
        }
        val file = outputFile
        outputFile = null
        val mr = recorder
        recorder = null
        var stopFailed = false
        try {
            mr?.stop()
        } catch (_: RuntimeException) {
            // Already stopped (e.g. max-duration) — still try to keep the file.
            stopFailed = true
        }
        runCatching { mr?.release() }
        val valid = file?.takeIf { it.exists() && it.length() > MIN_VALID_BYTES }
        if (valid == null && stopFailed) {
            file?.delete()
        }
        return valid
    }

    fun cancel() {
        finished.set(true)
        val file = outputFile
        outputFile = null
        val mr = recorder
        recorder = null
        try {
            mr?.stop()
        } catch (_: RuntimeException) {
            // Ignored — cancel after a very short press often throws.
        }
        runCatching { mr?.release() }
        file?.delete()
    }

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun release() {
        cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private companion object {
        const val AAC_BIT_RATE = 96_000
        const val AAC_SAMPLE_RATE = 44_100
        const val MIN_VALID_BYTES = 256L
    }
}
