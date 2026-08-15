@file:Suppress("ReturnCount")

package com.vault.vanishx.data.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

interface ClipboardInviteAccess {
    fun readPrimaryText(): String?
    fun clearPrimaryClip()
}

/**
 * Reads the primary clip after window focus (Android 10+).
 *
 * Android 12+ may show a one-time system toast that the app read the clipboard (E14-6).
 * VanishX only reads on cold start, not on every [android.app.Activity.onResume].
 */
class ClipboardInviteReader @Inject constructor(
    @ActivityContext private val context: Context,
) : ClipboardInviteAccess {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)

    override fun readPrimaryText(): String? {
        val clip = clipboard?.primaryClip ?: return null
        if (clip.itemCount < 1) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    override fun clearPrimaryClip() {
        val manager = clipboard ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.clearPrimaryClip()
        } else {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
