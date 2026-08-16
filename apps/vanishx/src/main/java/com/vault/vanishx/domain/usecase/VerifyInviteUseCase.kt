@file:Suppress("ReturnCount")

package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.InviteJoinCleanup
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.InvitePendingCodec
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RemoteRoomClock
import com.vault.vanishx.domain.model.RoomInvite
import javax.inject.Inject

/**
 * Invite verify (E14-5): [MailboxRemoteDataSource.readRoomMeta] only — no REST `/invites/verify`.
 */
class VerifyInviteUseCase @Inject constructor(
    private val remote: MailboxRemoteDataSource,
    private val pendingInviteStore: PendingInviteStore,
    private val joinCleanup: InviteJoinCleanup,
) {
    sealed interface Result {
        data class Live(val invite: RoomInvite, val meta: RemoteRoomMeta) : Result
        data class Dead(val message: String) : Result
    }

    suspend operator fun invoke(
        raw: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.Dead(EMPTY)
        val invite = InviteUriCodec.parse(trimmed)
        if (invite == null) {
            if (matchesPending(trimmed)) joinCleanup.clearPendingAndClipboard()
            return Result.Dead(INVALID)
        }
        val meta = runCatching { remote.readRoomMeta(invite.roomId) }.getOrNull()
        if (meta == null) {
            joinCleanup.clearPendingAndClipboard()
            return Result.Dead(NOT_FOUND)
        }
        if (RemoteRoomClock.isExpired(meta.expiresAt, meta.hostPro, nowMs)) {
            joinCleanup.clearPendingAndClipboard()
            return Result.Dead(EXPIRED)
        }
        return Result.Live(invite, meta)
    }

    private fun matchesPending(raw: String): Boolean {
        val pending = pendingInviteStore.peek() ?: return false
        if (pending == raw) return true
        val canonicalPending = InvitePendingCodec.canonicalize(pending)
        val canonicalRaw = InvitePendingCodec.canonicalize(raw)
        return canonicalPending != null && canonicalPending == canonicalRaw
    }

    companion object {
        const val EMPTY = "Invite is empty"
        const val INVALID = "Invalid invite link or code"
        const val NOT_FOUND = "This invite is no longer available"
        const val EXPIRED = "This invite has expired"
    }
}
