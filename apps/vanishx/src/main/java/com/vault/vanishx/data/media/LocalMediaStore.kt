package com.vault.vanishx.data.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** App-private media cache under [Context.getNoBackupFilesDir] (E11-8). */
@Singleton
class LocalMediaStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun write(roomId: String, messageId: String, attId: String, bytes: ByteArray): String {
        val directory = File(context.noBackupFilesDir, "media/$roomId/$messageId").apply { mkdirs() }
        return File(directory, attId).apply { writeBytes(bytes) }.absolutePath
    }

    fun wipeRoom(roomId: String) {
        File(context.noBackupFilesDir, "media/$roomId").deleteRecursively()
    }

    fun wipeMessage(roomId: String, messageId: String) {
        File(context.noBackupFilesDir, "media/$roomId/$messageId").deleteRecursively()
    }

    fun wipeAll() {
        File(context.noBackupFilesDir, "media").deleteRecursively()
    }
}
