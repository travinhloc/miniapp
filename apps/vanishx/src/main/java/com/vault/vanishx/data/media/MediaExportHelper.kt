@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "MagicNumber")

package com.vault.vanishx.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.vault.vanishx.data.crypto.RoomBlobCipher
import com.vault.vanishx.domain.model.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pro Save: encrypted blob → public **Download/VanishX** (same AES-GCM as room media).
 * File is not openable in Gallery/PDF apps — needs roomKey + VanishX to decrypt.
 */
@Singleton
class MediaExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blobCipher: RoomBlobCipher,
) {
    fun saveToDevice(message: ChatMessage, roomKey: String): Boolean {
        if (roomKey.isBlank()) return false
        val path = message.mediaLocalPath ?: return false
        val source = File(path)
        if (!source.exists()) return false
        val attId = message.mediaAttId?.takeIf { it.isNotBlank() } ?: message.id
        val plaintext = runCatching { source.readBytes() }.getOrNull() ?: return false
        if (plaintext.isEmpty()) return false
        val ciphertext = runCatching {
            blobCipher.encrypt(message.roomId, attId, roomKey, plaintext)
        }.getOrNull() ?: return false

        val displayName = encryptedDisplayName(message)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(displayName, ciphertext)
        } else {
            saveViaLegacyDownloads(displayName, ciphertext)
        }
    }

    private fun encryptedDisplayName(message: ChatMessage): String {
        val base = message.mediaFileName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "vanishx_${message.id}"
        val safe = base.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return "$safe$ENCRYPTED_SUFFIX"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(displayName: String, ciphertext: ByteArray): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, ENCRYPTED_MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DOWNLOAD_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out -> out.write(ciphertext) } ?: return false
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyDownloads(displayName: String, ciphertext: ByteArray): Boolean {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(root, FOLDER_NAME)
        if (!dir.exists() && !dir.mkdirs()) return false
        val target = File(dir, displayName)
        return runCatching {
            target.outputStream().use { it.write(ciphertext) }
            true
        }.getOrDefault(false)
    }

    companion object {
        const val FOLDER_NAME = "VanishX"
        /** MediaStore relative path under public Downloads. */
        const val RELATIVE_DOWNLOAD_DIR = "Download/$FOLDER_NAME"
        const val ENCRYPTED_SUFFIX = ".vxenc"
        const val ENCRYPTED_MIME = "application/octet-stream"
    }
}
