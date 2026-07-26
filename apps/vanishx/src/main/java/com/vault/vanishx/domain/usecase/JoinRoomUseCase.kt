package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
    private val roomPushTopics: RoomPushTopics,
) {
    suspend operator fun invoke(rawInvite: String): MailboxRoom {
        val invite = InviteUriCodec.parse(rawInvite)
            ?: error("Invalid invite link or code")
        return invoke(invite)
    }

    suspend operator fun invoke(invite: RoomInvite): MailboxRoom {
        mailboxRepository.getRoom(invite.roomId)?.let { existing ->
            val resolved = existing.copy(status = existing.resolvedStatus())
            if (resolved.status == MailboxRoom.STATUS_ACTIVE) {
                roomPushTopics.subscribe(resolved.id)
            }
            return resolved
        }

        val remoteMeta = runCatching { remote.readRoomMeta(invite.roomId) }.getOrNull()
        val now = System.currentTimeMillis()
        val createdAt = remoteMeta?.createdAt ?: now
        val expiresAt = invite.expiresAt ?: remoteMeta?.expiresAt ?: 0L
        val status = if (expiresAt > 0L && now >= expiresAt) {
            MailboxRoom.STATUS_EXPIRED
        } else {
            MailboxRoom.STATUS_ACTIVE
        }

        val room = MailboxRoom(
            id = invite.roomId,
            roomKey = invite.roomKey,
            createdAt = createdAt,
            expiresAt = expiresAt,
            status = status,
            role = MailboxRoom.ROLE_MEMBER,
        )
        mailboxRepository.upsertRoom(room)
        if (status == MailboxRoom.STATUS_ACTIVE) {
            roomPushTopics.subscribe(room.id)
        }
        return room
    }
}
