package com.vault.vanishx.data.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Clears pending HTTPS invite and best-effort clipboard after a dead token (story 14.5). */
@Singleton
class InviteJoinCleanup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingInviteStore: PendingInviteStore,
) {
    fun clearPendingAndClipboard() {
        pendingInviteStore.clear()
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.clearPrimaryClip()
            } else {
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
