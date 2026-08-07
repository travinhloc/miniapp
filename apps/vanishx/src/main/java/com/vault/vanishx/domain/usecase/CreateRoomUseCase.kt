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
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import javax.inject.Inject

data class CreatedRoom(
    val room: MailboxRoom,
    val invite: RoomInvite,
)

/**
 * Creates a room. Room clock is **not** started here:
 * - Free Host: `expiresAt = 0` until guest enters (activate)
 * - Pro Host: no room clock forever (`hostPro = true`, `expiresAt = 0`)
 *
 * [ttl] is reserved for Free activate duration (currently always [RoomTtlOption.ONE_DAY] at join).
 */
class CreateRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val secretsGenerator: RoomSecretsGenerator,
    private val roomPushTopics: RoomPushTopics,
    private val proEntitlement: ProEntitlementRepository,
) {
    @Suppress("UnusedParameter")
    suspend operator fun invoke(
        ttl: RoomTtlOption,
        title: String? = null,
        nickname: String? = null,
        icebreaker: String? = null,
    ): CreatedRoom {
        val identity = identityRepository.ensureIdentity()
        val now = System.currentTimeMillis()
        val hostPro = proEntitlement.isProNow()
        val trimmedIcebreaker = icebreaker
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(RemoteRoomMeta.MAX_ICEBREAKER_LENGTH)
        val room = MailboxRoom(
            id = secretsGenerator.newRoomId(),
            roomKey = secretsGenerator.newRoomKey(),
            createdAt = now,
            expiresAt = 0L,
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_CREATOR,
            icebreaker = trimmedIcebreaker,
            hostPro = hostPro,
            activatedAt = 0L,
        )
        remote.writeRoomMeta(
            roomId = room.id,
            meta = RemoteRoomMeta(
                createdAt = room.createdAt,
                expiresAt = 0L,
                creatorPub = identity.publicKeyBase64,
                icebreaker = trimmedIcebreaker,
                hostPro = hostPro,
                activatedAt = null,
            ),
        )
        mailboxRepository.upsertRoom(room)
        roomPushTopics.subscribe(room.id)
        return CreatedRoom(
            room = room,
            invite = RoomInvite(
                roomId = room.id,
                roomKey = room.roomKey,
                expiresAt = null,
            ),
        )
    }
}
