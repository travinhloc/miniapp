package com.vault.vanishx.data.remote

/**
 * Firebase RTDB mailbox access. Story 2.1 — ciphertext + TTL metadata only.
 */
interface MailboxRemoteDataSource {
    suspend fun ensureAuthenticated()

    suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta)

    suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage)

    suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage?

    suspend fun deleteMessage(roomId: String, messageId: String)
}
