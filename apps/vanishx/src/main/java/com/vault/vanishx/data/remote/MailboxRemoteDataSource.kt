package com.vault.vanishx.data.remote

/**
 * Firebase RTDB mailbox access — ciphertext + TTL metadata only.
 */
interface MailboxRemoteDataSource {
    suspend fun ensureAuthenticated()

    suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta)

    suspend fun readRoomMeta(roomId: String): RemoteRoomMeta?

    suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage)

    suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage?

    suspend fun listMessages(roomId: String): List<RemoteMailboxMessage>

    suspend fun deleteMessage(roomId: String, messageId: String)

    /**
     * Emits the full message list whenever children change. Caller must cancel the collection
     * to detach the listener.
     */
    fun observeMessages(roomId: String): kotlinx.coroutines.flow.Flow<List<RemoteMailboxMessage>>
}
