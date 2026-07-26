package com.vault.vanishx.data.remote

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory mailbox for unit tests (no Firebase).
 */
class InMemoryMailboxRemoteDataSource : MailboxRemoteDataSource {

    private var authenticated = false
    private val roomMeta = ConcurrentHashMap<String, RemoteRoomMeta>()
    private val messages = ConcurrentHashMap<String, RemoteMailboxMessage>()
    private val revisions = MutableStateFlow(0)

    override suspend fun ensureAuthenticated() {
        authenticated = true
    }

    override suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta) {
        ensureAuthenticated()
        roomMeta[roomId] = meta
    }

    override suspend fun readRoomMeta(roomId: String): RemoteRoomMeta? {
        ensureAuthenticated()
        return roomMeta[roomId]
    }

    override suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage) {
        require(message.ciphertext.length in 1..RemoteMailboxMessage.MAX_CIPHERTEXT_LENGTH)
        ensureAuthenticated()
        messages[key(roomId, message.messageId)] = message
        revisions.value = revisions.value + 1
    }

    override suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage? {
        ensureAuthenticated()
        return messages[key(roomId, messageId)]
    }

    override suspend fun listMessages(roomId: String): List<RemoteMailboxMessage> {
        ensureAuthenticated()
        val prefix = "$roomId/"
        return messages.entries
            .filter { it.key.startsWith(prefix) }
            .map { it.value }
    }

    override suspend fun deleteMessage(roomId: String, messageId: String) {
        ensureAuthenticated()
        messages.remove(key(roomId, messageId))
        revisions.value = revisions.value + 1
    }

    override fun observeMessages(roomId: String): Flow<List<RemoteMailboxMessage>> =
        revisions.map { listMessagesBlocking(roomId) }

    fun isAuthenticated(): Boolean = authenticated

    fun metaFor(roomId: String): RemoteRoomMeta? = roomMeta[roomId]

    private fun listMessagesBlocking(roomId: String): List<RemoteMailboxMessage> {
        val prefix = "$roomId/"
        return messages.entries
            .filter { it.key.startsWith(prefix) }
            .map { it.value }
    }

    private fun key(roomId: String, messageId: String) = "$roomId/$messageId"
}
