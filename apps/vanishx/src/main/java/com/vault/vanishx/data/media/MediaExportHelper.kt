@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount")

package com.vault.vanishx.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun saveToDevice(message: ChatMessage): Boolean {
        val path = message.mediaLocalPath ?: return false
        val source = File(path)
        if (!source.exists()) return false
        val mime = message.mediaMime ?: "application/octet-stream"
        val name = message.mediaFileName
            ?: "vanishx_${message.id}.${extension(mime, message.mediaKind)}"
        val collection = when (message.mediaKind) {
            AttachmentMeta.KIND_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            AttachmentMeta.KIND_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            AttachmentMeta.KIND_VOICE -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    private fun extension(mime: String, kind: String?): String = when {
        mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
        mime.contains("png") -> "png"
        mime.contains("mp4") && kind == AttachmentMeta.KIND_VOICE -> "m4a"
        mime.contains("aac") || mime.contains("m4a") -> "m4a"
        mime.contains("mp4") -> "mp4"
        mime.contains("pdf") -> "pdf"
        kind == AttachmentMeta.KIND_VOICE -> "m4a"
        kind == AttachmentMeta.KIND_VIDEO -> "mp4"
        kind == AttachmentMeta.KIND_IMAGE -> "jpg"
        else -> "bin"
    }
}
