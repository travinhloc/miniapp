@file:Suppress("MagicNumber", "ReturnCount", "ComplexMethod")

package com.vault.vanishx.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class VideoPreviewFrame(
    val bitmap: Bitmap,
    val durationMs: Long,
)

/** Local-only thumbnails for chat bubbles (ciphertext stays on Storage). */
@Singleton
class MediaPreviewLoader @Inject constructor() {

    fun videoFrame(absolutePath: String, maxEdge: Int = DEFAULT_MAX_EDGE): VideoPreviewFrame? {
        val file = File(absolutePath)
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            VideoPreviewFrame(bitmap = scaleDown(frame, maxEdge), durationMs = duration)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun pdfFirstPage(absolutePath: String, maxEdge: Int = DEFAULT_MAX_EDGE): Bitmap? {
        val file = File(absolutePath)
        if (!file.exists()) return null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount < 1) return null
            renderer.openPage(0).use { page ->
                val scale = maxEdge.toFloat() / maxOf(page.width, page.height).coerceAtLeast(1)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }

    /** Duration for voice/video from absolute path or content URI string. */
    fun mediaDurationMs(context: Context, path: String): Long {
        if (path.isBlank()) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            val file = File(path)
            if (file.exists()) {
                retriever.setDataSource(file.absolutePath)
            } else {
                retriever.setDataSource(context, Uri.parse(path))
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun textSnippet(absolutePath: String, maxChars: Int = TEXT_SNIPPET_CHARS): String? {
        val file = File(absolutePath)
        if (!file.exists()) return null
        return try {
            file.bufferedReader().use { reader ->
                buildString {
                    var remaining = maxChars
                    while (remaining > 0) {
                        val line = reader.readLine() ?: break
                        if (isNotEmpty()) append('\n')
                        val take = line.take(remaining)
                        append(take)
                        remaining -= take.length + 1
                    }
                }.trim().ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    companion object {
        const val DEFAULT_MAX_EDGE = 720
        const val TEXT_SNIPPET_CHARS = 280

        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0L) return "00:00"
            val totalSec = (durationMs / 1000L).toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return "%02d:%02d".format(m, s)
        }

        fun fileTypeLabel(mime: String?, fileName: String?): String {
            val fromName = fileName?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() }
            if (!fromName.isNullOrBlank()) {
                return when (fromName) {
                    "MD", "MARKDOWN" -> "MD"
                    "TXT" -> "TXT"
                    "DOC", "DOCX" -> "DOC"
                    "XLS", "XLSX" -> "XLS"
                    "PDF" -> "PDF"
                    else -> fromName.take(4)
                }
            }
            val m = mime.orEmpty().lowercase()
            return when {
                m.contains("pdf") -> "PDF"
                m.contains("word") || m.contains("msword") -> "DOC"
                m.contains("sheet") || m.contains("excel") -> "XLS"
                m.contains("markdown") -> "MD"
                m.startsWith("text/") -> "TXT"
                else -> "FILE"
            }
        }
    }
}
