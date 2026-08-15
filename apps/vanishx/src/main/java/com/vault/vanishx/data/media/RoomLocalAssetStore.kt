package com.vault.vanishx.data.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Local-only room avatar / wallpaper files (Epic 12 — no upload). */
@Singleton
class RoomLocalAssetStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun copyAvatar(roomId: String, source: Uri): String {
        val dir = roomDir(roomId)
        dir.mkdirs()
        val dest = File(dir, "avatar.jpg")
        copyBounded(source, dest, MAX_AVATAR_BYTES)
        return dest.absolutePath
    }

    fun copyWallpaper(roomId: String, source: Uri): String {
        val dir = roomDir(roomId)
        dir.mkdirs()
        val dest = File(dir, "wallpaper.jpg")
        copyBounded(source, dest, MAX_WALLPAPER_BYTES)
        return dest.absolutePath
    }

    fun clearAvatar(roomId: String) {
        File(roomDir(roomId), "avatar.jpg").delete()
    }

    fun clearWallpaper(roomId: String) {
        File(roomDir(roomId), "wallpaper.jpg").delete()
    }

    fun wipeRoom(roomId: String) {
        roomDir(roomId).deleteRecursively()
    }

    private fun copyBounded(source: Uri, dest: File, maxBytes: Long) {
        val input = context.contentResolver.openInputStream(source)
            ?: error("Cannot read file")
        input.use { stream ->
            dest.outputStream().use { output ->
                copyStreamBounded(stream, output, dest, maxBytes)
            }
        }
    }

    private fun copyStreamBounded(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        dest: File,
        maxBytes: Long,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                dest.delete()
                error("File too large")
            }
            output.write(buffer, 0, read)
        }
    }

    private fun roomDir(roomId: String): File =
        File(context.noBackupFilesDir, "room_prefs/$roomId")

    companion object {
        const val MAX_AVATAR_BYTES = 10L * 1024L * 1024L
        const val MAX_WALLPAPER_BYTES = 15L * 1024L * 1024L
    }
}
