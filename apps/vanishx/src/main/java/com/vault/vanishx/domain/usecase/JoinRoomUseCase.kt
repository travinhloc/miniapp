package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

/**
 * Guest Accept & enter. **Activate** starts here:
 * - Free Host: first enter sets `activatedAt` + `expiresAt = now + 24h`
 * - Pro Host: sets `activatedAt` only — no room clock
 */
class JoinRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
    private val roomPushTopics: RoomPushTopics,
    private val blockRepository: BlockRepository,
) {
    suspend operator fun invoke(rawInvite: String, nickname: String? = null): MailboxRoom {
        val invite = InviteUriCodec.parse(rawInvite)
            ?: error("Invalid invite link or code")
        return invoke(invite, nickname)
    }

    suspend operator fun invoke(invite: RoomInvite, nickname: String? = null): MailboxRoom {
        mailboxRepository.getRoom(invite.roomId)?.let { existing ->
            val updated = if (!nickname.isNullOrBlank() && existing.nickname != nickname) {
                existing.copy(nickname = nickname.trim()).also { mailboxRepository.upsertRoom(it) }
            } else {
                existing
            }
            return reopenExisting(updated)
        }

        val remoteMeta = runCatching { remote.readRoomMeta(invite.roomId) }.getOrNull()
            ?: error("Room not found on mailbox")
        rejectIfBlocked(remoteMeta.creatorPub)

        val activatedMeta = activateIfNeeded(invite.roomId, remoteMeta)
        val room = buildMemberRoom(invite, activatedMeta, nickname)
        mailboxRepository.upsertRoom(room)
        if (room.status == MailboxRoom.STATUS_ACTIVE) {
            roomPushTopics.subscribe(room.id)
        }
        return room
    }

    private suspend fun activateIfNeeded(roomId: String, meta: RemoteRoomMeta): RemoteRoomMeta {
        val already = (meta.activatedAt ?: 0L) > 0L
        if (already) return meta

        val now = System.currentTimeMillis()
        val activated = meta.copy(
            activatedAt = now,
            expiresAt = if (meta.hostPro) {
                0L
            } else {
                now + RoomTtlOption.ONE_DAY.durationMs
            },
        )
        remote.writeRoomMeta(roomId, activated)
        return activated
    }

    private suspend fun reopenExisting(existing: MailboxRoom): MailboxRoom {
        val meta = runCatching { remote.readRoomMeta(existing.id) }.getOrNull()
        val merged = if (meta != null) {
            existing.copy(
                expiresAt = meta.expiresAt,
                hostPro = meta.hostPro,
                activatedAt = meta.activatedAt ?: existing.activatedAt,
                icebreaker = meta.icebreaker ?: existing.icebreaker,
                peerPub = existing.peerPub ?: meta.creatorPub,
                status = existing.resolvedStatus(),
            ).also { mailboxRepository.upsertRoom(it) }
        } else {
            existing.copy(status = existing.resolvedStatus())
        }
        if (merged.status == MailboxRoom.STATUS_ACTIVE) {
            roomPushTopics.subscribe(merged.id)
        }
        return merged
    }

    private suspend fun rejectIfBlocked(creatorPub: String?) {
        if (creatorPub != null && blockRepository.isBlocked(creatorPub)) {
            error("Peer is blocked")
        }
    }

    private fun buildMemberRoom(
        invite: RoomInvite,
        remoteMeta: RemoteRoomMeta,
        nickname: String?,
    ): MailboxRoom {
        val now = System.currentTimeMillis()
        val expiresAt = remoteMeta.expiresAt
        val status = if (isRemoteRoomExpired(expiresAt, remoteMeta.hostPro, now)) {
            MailboxRoom.STATUS_EXPIRED
        } else {
            MailboxRoom.STATUS_ACTIVE
        }
        return MailboxRoom(
            id = invite.roomId,
            roomKey = invite.roomKey,
            createdAt = remoteMeta.createdAt,
            expiresAt = expiresAt,
            nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
            status = status,
            role = MailboxRoom.ROLE_MEMBER,
            peerPub = remoteMeta.creatorPub,
            icebreaker = remoteMeta.icebreaker,
            hostPro = remoteMeta.hostPro,
            activatedAt = remoteMeta.activatedAt ?: 0L,
        )
    }

    private fun isRemoteRoomExpired(expiresAt: Long, hostPro: Boolean, nowMs: Long): Boolean {
        if (hostPro || expiresAt <= 0L) return false
        return nowMs >= expiresAt
    }
}
