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
    private val reports = ConcurrentHashMap<String, RemoteReport>()
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

    override suspend fun deleteAllMessages(roomId: String) {
        ensureAuthenticated()
        val prefix = "$roomId/"
        messages.keys.filter { it.startsWith(prefix) }.forEach { messages.remove(it) }
        revisions.value = revisions.value + 1
    }

    override suspend fun writeReport(report: RemoteReport) {
        require(report.roomId.length in 1..RemoteReport.MAX_ROOM_ID_LENGTH)
        require(report.reporterPub.length in 1..RemoteReport.MAX_PUB_LENGTH)
        report.peerPub?.let { require(it.length in 1..RemoteReport.MAX_PUB_LENGTH) }
        report.reason?.let { require(it.length <= RemoteReport.MAX_REASON_LENGTH) }
        ensureAuthenticated()
        reports[report.reportId] = report
    }

    override fun observeMessages(roomId: String): Flow<List<RemoteMailboxMessage>> =
        revisions.map { listMessagesBlocking(roomId) }

    fun isAuthenticated(): Boolean = authenticated

    fun metaFor(roomId: String): RemoteRoomMeta? = roomMeta[roomId]

    fun reportFor(reportId: String): RemoteReport? = reports[reportId]

    fun allReports(): List<RemoteReport> = reports.values.toList()

    private fun listMessagesBlocking(roomId: String): List<RemoteMailboxMessage> {
        val prefix = "$roomId/"
        return messages.entries
            .filter { it.key.startsWith(prefix) }
            .map { it.value }
    }

    private fun key(roomId: String, messageId: String) = "$roomId/$messageId"
}
