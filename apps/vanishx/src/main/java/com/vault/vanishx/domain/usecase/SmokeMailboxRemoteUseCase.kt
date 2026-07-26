package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.data.remote.RemoteRoomMeta
import java.util.UUID
import javax.inject.Inject

/**
 * Staging/debug helper: write → read → delete one fake ciphertext node on RTDB.
 */
class SmokeMailboxRemoteUseCase @Inject constructor(
    private val remote: MailboxRemoteDataSource,
) {
    suspend operator fun invoke(): String {
        val now = System.currentTimeMillis()
        val roomId = "smoke_${UUID.randomUUID().toString().take(SMOKE_ID_CHARS)}"
        val messageId = "msg_${UUID.randomUUID().toString().take(SMOKE_ID_CHARS)}"
        val expiresAt = now + TTL_MS

        remote.ensureAuthenticated()
        remote.writeRoomMeta(
            roomId = roomId,
            meta = RemoteRoomMeta(
                createdAt = now,
                expiresAt = expiresAt,
                creatorPub = "smoke_pub",
            ),
        )
        remote.writeMessage(
            roomId = roomId,
            message = RemoteMailboxMessage(
                messageId = messageId,
                ciphertext = RemoteMailboxMessage.SMOKE_CIPHERTEXT,
                senderPub = "smoke_pub",
                createdAt = now,
                expiresAt = expiresAt,
            ),
        )
        val read = remote.readMessage(roomId, messageId)
            ?: error("smoke read returned null")
        check(read.ciphertext == RemoteMailboxMessage.SMOKE_CIPHERTEXT) {
            "smoke ciphertext mismatch"
        }
        remote.deleteMessage(roomId, messageId)
        check(remote.readMessage(roomId, messageId) == null) {
            "smoke delete did not remove message"
        }
        return "ok $roomId/$messageId"
    }

    private companion object {
        const val SMOKE_ID_CHARS = 8
        const val TTL_MS = 60 * 60 * 1000L
    }
}
