package com.vault.vanishx.data.remote

/**
 * Firebase RTDB mailbox access — ciphertext + TTL metadata only.
 */
@Suppress("TooManyFunctions")
interface MailboxRemoteDataSource {
    suspend fun ensureAuthenticated()

    suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta)

    suspend fun readRoomMeta(roomId: String): RemoteRoomMeta?

    fun observeRoomMeta(roomId: String): kotlinx.coroutines.flow.Flow<RemoteRoomMeta?>

    suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage)

    suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage?

    suspend fun listMessages(roomId: String): List<RemoteMailboxMessage>

    suspend fun deleteMessage(roomId: String, messageId: String)

    /** Removes all message children for [roomId]; room meta is left intact. */
    suspend fun deleteAllMessages(roomId: String)

    /** Writes a one-shot UGC report under `/reports/{reportId}`. */
    suspend fun writeReport(report: RemoteReport)

    /**
     * Emits the full message list whenever children change. Caller must cancel the collection
     * to detach the listener.
     */
    fun observeMessages(roomId: String): kotlinx.coroutines.flow.Flow<List<RemoteMailboxMessage>>

    // Epic 9 engagement metadata (no plaintext)
    suspend fun setPresence(roomId: String, deviceId: String, online: Boolean)
    fun observePresence(roomId: String): kotlinx.coroutines.flow.Flow<List<RemotePresence>>

    suspend fun setReadWatermark(roomId: String, deviceId: String, messageId: String)
    fun observeReadWatermarks(roomId: String): kotlinx.coroutines.flow.Flow<List<RemoteReadWatermark>>

    suspend fun setTyping(roomId: String, deviceId: String, atMs: Long)
    suspend fun clearTyping(roomId: String, deviceId: String)
    fun observeTyping(roomId: String): kotlinx.coroutines.flow.Flow<List<RemoteTyping>>

    suspend fun setReaction(roomId: String, messageId: String, deviceId: String, emoji: String)
    suspend fun clearReaction(roomId: String, messageId: String, deviceId: String)
    fun observeReactions(roomId: String): kotlinx.coroutines.flow.Flow<List<RemoteReaction>>
}
