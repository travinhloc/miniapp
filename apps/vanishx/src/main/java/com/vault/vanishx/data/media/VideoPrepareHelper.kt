package com.vault.vanishx.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import com.vault.vanishx.domain.model.MediaLimits
import java.io.File

/**
 * Pre-send video gate (E16-6 · E16-7).
 *
 * Capture uses CameraX SD / lowest quality fallback so a 60s clip typically stays under
 * [MediaLimits.VIDEO_MAX_BYTES]. There is no MediaCodec re-encode pipeline in this story —
 * files already under the cap pass through; oversized clips fail with a clear error.
 */
class VideoPrepareHelper(private val context: Context) {

    fun createCaptureFile(): File {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        return File(dir, "video_${System.currentTimeMillis()}.mp4")
    }

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Validate duration ≤ [MediaLimits.VIDEO_MAX_DURATION_MS] and size ≤
     * [MediaLimits.VIDEO_MAX_BYTES]. Returns [source] unchanged when valid.
     */
    fun prepareForSend(source: File): File {
        require(source.exists() && source.length() >= MIN_VALID_BYTES) {
            "Recording empty or too short"
        }
        val durationMs = readDurationMs(source)
        require(durationMs in 1..MediaLimits.VIDEO_MAX_DURATION_MS) {
            "Video duration must be ≤ ${MediaLimits.VIDEO_MAX_DURATION_MS / MS_PER_SEC}s"
        }
        require(source.length() <= MediaLimits.VIDEO_MAX_BYTES) {
            "Video exceeds ${MediaLimits.VIDEO_MAX_BYTES / BYTES_PER_MB} MB. Try a shorter clip."
        }
        return source
    }

    fun readDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val MIN_VALID_BYTES = 1024L
        const val MS_PER_SEC = 1000L
        const val BYTES_PER_MB = 1024L * 1024L
    }
}
