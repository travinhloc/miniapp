package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import java.util.UUID
import javax.inject.Inject

class SendRoomMessageUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val cipher: RoomMessageCipher,
) {
    suspend operator fun invoke(roomId: String, plaintext: String): ChatMessage {
        val text = plaintext.trim()
        require(text.isNotEmpty()) { "Message is empty" }

        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        val resolved = room.copy(status = room.resolvedStatus())
        if (resolved.status == MailboxRoom.STATUS_EXPIRED) {
            mailboxRepository.upsertRoom(resolved)
            error("Room expired")
        }

        val identity = identityRepository.ensureIdentity()
        val now = System.currentTimeMillis()
        val messageId = "m_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ciphertext = cipher.encrypt(roomId, room.roomKey, text)
        val remoteMessage = RemoteMailboxMessage(
            messageId = messageId,
            ciphertext = ciphertext,
            senderPub = identity.publicKeyBase64,
            createdAt = now,
            expiresAt = room.expiresAt,
        )
        remote.writeMessage(roomId, remoteMessage)

        val local = ChatMessage(
            id = messageId,
            roomId = roomId,
            body = text,
            sentAt = now,
            expiresAt = room.expiresAt,
            direction = ChatMessage.DIRECTION_OUT,
        )
        mailboxRepository.upsertMessage(local)
        return local
    }
}
