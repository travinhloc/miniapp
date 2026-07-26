package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomSecretsGenerator
import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

data class CreatedRoom(
    val room: MailboxRoom,
    val invite: RoomInvite,
)

class CreateRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val secretsGenerator: RoomSecretsGenerator,
    private val roomPushTopics: RoomPushTopics,
) {
    suspend operator fun invoke(ttl: RoomTtlOption): CreatedRoom {
        val identity = identityRepository.ensureIdentity()
        val now = System.currentTimeMillis()
        val expiresAt = now + ttl.durationMs
        val room = MailboxRoom(
            id = secretsGenerator.newRoomId(),
            roomKey = secretsGenerator.newRoomKey(),
            createdAt = now,
            expiresAt = expiresAt,
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_CREATOR,
        )
        remote.writeRoomMeta(
            roomId = room.id,
            meta = RemoteRoomMeta(
                createdAt = room.createdAt,
                expiresAt = room.expiresAt,
                creatorPub = identity.publicKeyBase64,
            ),
        )
        mailboxRepository.upsertRoom(room)
        roomPushTopics.subscribe(room.id)
        return CreatedRoom(
            room = room,
            invite = RoomInvite(
                roomId = room.id,
                roomKey = room.roomKey,
                expiresAt = room.expiresAt,
            ),
        )
    }
}
