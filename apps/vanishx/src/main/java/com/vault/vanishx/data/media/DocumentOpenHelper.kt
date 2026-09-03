package com.vault.vanishx.data.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentOpenHelper @Inject constructor() {
    @Suppress("ReturnCount")
    fun openWithSystemApp(context: Context, localPath: String, mime: String?): Boolean {
        val file = File(localPath).takeIf { it.exists() } ?: return false
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(context, authority, file)
        }.getOrElse { return false }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, null))
            true
        }.getOrDefault(false)
    }
}
