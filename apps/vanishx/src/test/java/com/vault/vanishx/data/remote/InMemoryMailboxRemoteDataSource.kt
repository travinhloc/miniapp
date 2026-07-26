package com.vault.vanishx.data.remote

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory mailbox for unit tests (no Firebase).
 */
class InMemoryMailboxRemoteDataSource : MailboxRemoteDataSource {

    private var authenticated = false
    private val roomMeta = ConcurrentHashMap<String, RemoteRoomMeta>()
    private val messages = ConcurrentHashMap<String, RemoteMailboxMessage>()

    override suspend fun ensureAuthenticated() {
        authenticated = true
    }

    override suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta) {
        ensureAuthenticated()
        roomMeta[roomId] = meta
    }

    override suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage) {
        require(message.ciphertext.length in 1..RemoteMailboxMessage.MAX_CIPHERTEXT_LENGTH)
        ensureAuthenticated()
        messages[key(roomId, message.messageId)] = message
    }

    override suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage? {
        ensureAuthenticated()
        return messages[key(roomId, messageId)]
    }

    override suspend fun deleteMessage(roomId: String, messageId: String) {
        ensureAuthenticated()
        messages.remove(key(roomId, messageId))
    }

    fun isAuthenticated(): Boolean = authenticated

    fun metaFor(roomId: String): RemoteRoomMeta? = roomMeta[roomId]

    private fun key(roomId: String, messageId: String) = "$roomId/$messageId"
}
