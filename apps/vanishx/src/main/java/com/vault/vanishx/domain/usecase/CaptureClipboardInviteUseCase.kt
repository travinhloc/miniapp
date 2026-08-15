package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.ClipboardInviteAccess
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InviteClipboardParser
import com.vault.vanishx.domain.model.InviteUriCodec
import timber.log.Timber
import javax.inject.Inject

/**
 * Cold-start clipboard invite (story 14.3). Call only once per process, and only when
 * no native App Link / custom-scheme URI was captured.
 */
class CaptureClipboardInviteUseCase @Inject constructor(
    private val clipboard: ClipboardInviteAccess,
    private val pendingInviteStore: PendingInviteStore,
) {
    operator fun invoke(nowMs: Long = System.currentTimeMillis()): Boolean {
        val parsed = InviteClipboardParser.parse(clipboard.readPrimaryText(), nowMs)
        return when (parsed) {
            InviteClipboardParser.Result.Ignore -> false
            is InviteClipboardParser.Result.Discard -> {
                clipboard.clearPrimaryClip()
                Timber.w("Clipboard invite discarded (%s)", parsed.reason)
                false
            }
            is InviteClipboardParser.Result.Valid -> {
                clipboard.clearPrimaryClip()
                pendingInviteStore.save(InviteUriCodec.format(parsed.invite))
                true
            }
        }
    }
}
