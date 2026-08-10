@file:Suppress("LongParameterList", "LongMethod")

package com.vault.vanishx.domain.usecase

import android.net.Uri
import com.vault.vanishx.data.crypto.RoomBlobCipher
import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.media.LocalMediaStore
import com.vault.vanishx.data.media.MediaContentLoader
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.MediaStorageRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.domain.model.MessagePlaintextCodec
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import java.util.UUID
import javax.inject.Inject

class SendRoomMediaUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val mediaStorage: MediaStorageRemoteDataSource,
    private val cipher: RoomMessageCipher,
    private val blobCipher: RoomBlobCipher,
    private val mediaLoader: MediaContentLoader,
    private val localMediaStore: LocalMediaStore,
) {
    suspend operator fun invoke(
        roomId: String,
        uri: Uri,
        declaredMime: String?,
        displayName: String? = null,
    ): ChatMessage {
        val room = mailboxRepository.getRoom(roomId) ?: error("Room not found")
        val resolved = room.copy(status = room.resolvedStatus())
        if (resolved.status == MailboxRoom.STATUS_EXPIRED) {
            mailboxRepository.upsertRoom(resolved)
            error("Room expired")
        }
        if (resolved.isPendingActivation()) {
            error("Room not activated yet")
        }

        val loaded = mediaLoader.load(uri, declaredMime).let { media ->
            if (!displayName.isNullOrBlank() && media.fileName.isNullOrBlank()) {
                media.copy(fileName = displayName)
            } else {
                media
            }
        }
        require(loaded.bytes.size <= MediaLimits.maxBytesForKind(loaded.kind)) {
            "Media exceeds size limit"
        }

        val identity = identityRepository.ensureIdentity()
        val now = System.currentTimeMillis()
        val messageId = "m_${UUID.randomUUID().toString().replace("-", "").take(MESSAGE_ID_CHARS)}"
        val attId = "a_${UUID.randomUUID().toString().replace("-", "").take(ATT_ID_CHARS)}"

        val ciphertextBlob = blobCipher.encrypt(roomId, attId, room.roomKey, loaded.bytes)
        remote.ensureAuthenticated()
        mediaStorage.upload(roomId, messageId, attId, ciphertextBlob)

        val meta = AttachmentMeta(
            kind = loaded.kind,
            mime = loaded.mime,
            bytes = loaded.bytes.size.toLong(),
            attId = attId,
            width = loaded.width,
            height = loaded.height,
            fileName = loaded.fileName,
        )
        val envelope = MessagePlaintextCodec.encodeAttachment(meta)
        val wire = cipher.encrypt(roomId, room.roomKey, envelope)
        val wireExpiresAt = wireExpiresAt(resolved, now)
        remote.writeMessage(
            roomId,
            RemoteMailboxMessage(
                messageId = messageId,
                ciphertext = wire,
                senderPub = identity.publicKeyBase64,
                createdAt = now,
                expiresAt = wireExpiresAt,
            ),
        )

        val localPath = localMediaStore.write(roomId, messageId, attId, loaded.bytes)
        val local = ChatMessage(
            id = messageId,
            roomId = roomId,
            body = "",
            sentAt = now,
            expiresAt = wireExpiresAt,
            direction = ChatMessage.DIRECTION_OUT,
            mediaKind = meta.kind,
            mediaMime = meta.mime,
            mediaBytes = meta.bytes,
            mediaAttId = meta.attId,
            mediaWidth = meta.width,
            mediaHeight = meta.height,
            mediaFileName = meta.fileName,
            mediaLocalPath = localPath,
            mediaTransferStatus = ChatMessage.MEDIA_READY,
        )
        mailboxRepository.upsertMessage(local)
        return local
    }

    private fun wireExpiresAt(room: MailboxRoom, nowMs: Long): Long =
        when {
            room.hasRoomClock() -> room.expiresAt
            room.hostPro -> nowMs + ETERNAL_MESSAGE_TTL_MS
            else -> error("Room not activated yet")
        }

    private companion object {
        const val MESSAGE_ID_CHARS = 16
        const val ATT_ID_CHARS = 12
        const val ETERNAL_MESSAGE_TTL_MS = 365L * 24 * 60 * 60 * 1000 * 10
    }
}
