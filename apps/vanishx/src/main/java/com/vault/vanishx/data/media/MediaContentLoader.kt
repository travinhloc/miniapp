@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "MagicNumber", "LongMethod")

package com.vault.vanishx.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.MediaLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class LoadedMedia(
    val kind: String,
    val bytes: ByteArray,
    val mime: String,
    val fileName: String?,
    val width: Int? = null,
    val height: Int? = null,
)

@Singleton
class MediaContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor,
) {
    fun load(uri: Uri, declaredMime: String?): LoadedMedia {
        val displayName = queryDisplayName(uri)
        val mime = (declaredMime?.takeIf { it.isNotBlank() }
            ?: context.contentResolver.getType(uri)
            ?: guessMime(uri, displayName))
            .lowercase()
            .substringBefore(';')
            .trim()
        val kind = MediaLimits.kindForMimeOrName(mime, displayName)
            ?: error("Unsupported media type")
        val resolvedMime = when {
            kind == AttachmentMeta.KIND_IMAGE && mime == "application/octet-stream" -> "image/jpeg"
            kind == AttachmentMeta.KIND_VIDEO && mime == "application/octet-stream" -> "video/mp4"
            kind == AttachmentMeta.KIND_FILE &&
                mime == "application/octet-stream" &&
                MediaLimits.isPlainTextDocument(mime, displayName) -> {
                val ext = displayName?.substringAfterLast('.', "")?.lowercase()
                if (ext == "md" || ext == "markdown") "text/markdown" else "text/plain"
            }
            else -> mime
        }
        return when (kind) {
            AttachmentMeta.KIND_IMAGE -> {
                val compressed = imageCompressor.compress(uri)
                LoadedMedia(
                    kind = kind,
                    bytes = compressed.bytes,
                    mime = compressed.mime,
                    fileName = displayName,
                    width = compressed.width.takeIf { it > 0 },
                    height = compressed.height.takeIf { it > 0 },
                )
            }
            AttachmentMeta.KIND_VIDEO -> {
                validateDuration(
                    uri = uri,
                    maxMs = MediaLimits.VIDEO_MAX_DURATION_MS,
                    label = "Video",
                )
                val bytes = readBytes(uri, MediaLimits.VIDEO_MAX_BYTES)
                LoadedMedia(
                    kind = kind,
                    bytes = bytes,
                    mime = resolvedMime,
                    fileName = displayName,
                )
            }
            AttachmentMeta.KIND_VOICE -> {
                validateDuration(
                    uri = uri,
                    maxMs = MediaLimits.VOICE_MAX_DURATION_MS,
                    label = "Voice",
                )
                val bytes = readBytes(uri, MediaLimits.VOICE_MAX_BYTES)
                val voiceMime = when {
                    resolvedMime in MediaLimits.VOICE_MIME -> resolvedMime
                    resolvedMime == "application/octet-stream" -> "audio/mp4"
                    else -> "audio/mp4"
                }
                LoadedMedia(
                    kind = kind,
                    bytes = bytes,
                    mime = voiceMime,
                    fileName = displayName,
                )
            }
            else -> {
                val bytes = readBytes(uri, MediaLimits.FILE_MAX_BYTES)
                LoadedMedia(
                    kind = kind,
                    bytes = bytes,
                    mime = resolvedMime,
                    fileName = displayName,
                )
            }
        }
    }

    private fun validateDuration(uri: Uri, maxMs: Long, label: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            require(duration in 1..maxMs) {
                "$label duration must be ≤ ${maxMs / MS_PER_SEC}s"
            }
        } finally {
            retriever.release()
        }
    }

    private fun readBytes(uri: Uri, maxBytes: Long): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Unable to open content")
        return stream.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            val out = ArrayList<Byte>()
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "File exceeds $maxBytes bytes" }
                for (i in 0 until read) out.add(buffer[i])
            }
            out.toByteArray()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index < 0) return null
            return it.getString(index)
        }
    }

    private fun guessMime(uri: Uri, displayName: String?): String {
        val ext = displayName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.lowercase()
            ?: "application/octet-stream"
    }

    private companion object {
        const val DEFAULT_BUFFER = 16 * 1024
        const val MS_PER_SEC = 1_000L
    }
}
