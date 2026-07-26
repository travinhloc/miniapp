package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
    private val roomPushTopics: RoomPushTopics,
    private val blockRepository: BlockRepository,
) {
    suspend operator fun invoke(rawInvite: String): MailboxRoom {
        val invite = InviteUriCodec.parse(rawInvite)
            ?: error("Invalid invite link or code")
        return invoke(invite)
    }

    suspend operator fun invoke(invite: RoomInvite): MailboxRoom {
        mailboxRepository.getRoom(invite.roomId)?.let { return reopenExisting(it) }

        val remoteMeta = runCatching { remote.readRoomMeta(invite.roomId) }.getOrNull()
        rejectIfBlocked(remoteMeta?.creatorPub)

        val room = buildMemberRoom(invite, remoteMeta)
        mailboxRepository.upsertRoom(room)
        if (room.status == MailboxRoom.STATUS_ACTIVE) {
            roomPushTopics.subscribe(room.id)
        }
        return room
    }

    private suspend fun reopenExisting(existing: MailboxRoom): MailboxRoom {
        val resolved = existing.copy(status = existing.resolvedStatus())
        if (resolved.status == MailboxRoom.STATUS_ACTIVE) {
            roomPushTopics.subscribe(resolved.id)
        }
        return resolved
    }

    private suspend fun rejectIfBlocked(creatorPub: String?) {
        if (creatorPub != null && blockRepository.isBlocked(creatorPub)) {
            error("Peer is blocked")
        }
    }

    private fun buildMemberRoom(invite: RoomInvite, remoteMeta: RemoteRoomMeta?): MailboxRoom {
        val now = System.currentTimeMillis()
        val createdAt = remoteMeta?.createdAt ?: now
        val expiresAt = invite.expiresAt ?: remoteMeta?.expiresAt ?: 0L
        val status = if (expiresAt > 0L && now >= expiresAt) {
            MailboxRoom.STATUS_EXPIRED
        } else {
            MailboxRoom.STATUS_ACTIVE
        }
        return MailboxRoom(
            id = invite.roomId,
            roomKey = invite.roomKey,
            createdAt = createdAt,
            expiresAt = expiresAt,
            status = status,
            role = MailboxRoom.ROLE_MEMBER,
            peerPub = remoteMeta?.creatorPub,
        )
    }
}
