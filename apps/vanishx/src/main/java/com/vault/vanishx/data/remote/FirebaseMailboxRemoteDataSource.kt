@file:Suppress("LargeClass", "TooManyFunctions", "ReturnCount")

package com.vault.vanishx.data.remote

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miniapp.core.common.DispatchersProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMailboxRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val dispatchersProvider: DispatchersProvider,
) : MailboxRemoteDataSource {

    override suspend fun ensureAuthenticated() = withContext(dispatchersProvider.io) {
        requireDatabaseConfigured()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        Unit
    }

    private fun requireDatabaseConfigured() {
        val databaseUrl = FirebaseApp.getInstance().options.databaseUrl
        check(!databaseUrl.isNullOrBlank()) {
            "google-services.json thiếu firebase_url. " +
                "Tạo Realtime Database trên Firebase Console, rồi tải lại google-services.json " +
                "vào apps/vanishx/src/staging/ (thay file cũ)."
        }
    }

    override suspend fun writeRoomMeta(roomId: String, meta: RemoteRoomMeta) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            val payload = buildMap<String, Any> {
                put(KEY_CREATED_AT, meta.createdAt)
                put(KEY_EXPIRES_AT, meta.expiresAt)
                put(KEY_HOST_PRO, meta.hostPro)
                meta.creatorPub?.let { put(KEY_CREATOR_PUB, it) }
                meta.icebreaker?.let { put(KEY_ICEBREAKER, it) }
                meta.activatedAt?.takeIf { it > 0L }?.let { put(KEY_ACTIVATED_AT, it) }
            }
            roomRef(roomId).child(PATH_META).setValue(payload).await()
        }
    }

    override suspend fun readRoomMeta(roomId: String): RemoteRoomMeta? {
        ensureAuthenticated()
        return withContext(dispatchersProvider.io) {
            val snapshot = roomRef(roomId).child(PATH_META).get().await()
            if (!snapshot.exists()) return@withContext null
            parseRoomMeta(snapshot)
        }
    }

    override fun observeRoomMeta(roomId: String): Flow<RemoteRoomMeta?> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_META)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseRoomMeta(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun writeMessage(roomId: String, message: RemoteMailboxMessage) {
        require(message.ciphertext.length in 1..RemoteMailboxMessage.MAX_CIPHERTEXT_LENGTH) {
            "ciphertext length must be 1..${RemoteMailboxMessage.MAX_CIPHERTEXT_LENGTH}"
        }
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            val payload = mapOf(
                KEY_CIPHERTEXT to message.ciphertext,
                KEY_SENDER_PUB to message.senderPub,
                KEY_CREATED_AT to message.createdAt,
                KEY_EXPIRES_AT to message.expiresAt,
            )
            roomRef(roomId).child(PATH_MESSAGES).child(message.messageId).setValue(payload).await()
        }
    }

    override suspend fun readMessage(roomId: String, messageId: String): RemoteMailboxMessage? {
        ensureAuthenticated()
        return withContext(dispatchersProvider.io) {
            val snapshot = roomRef(roomId).child(PATH_MESSAGES).child(messageId).get().await()
            parseMessage(snapshot, messageId)
        }
    }

    override suspend fun listMessages(roomId: String): List<RemoteMailboxMessage> {
        ensureAuthenticated()
        return withContext(dispatchersProvider.io) {
            val snapshot = roomRef(roomId).child(PATH_MESSAGES).get().await()
            snapshot.children.mapNotNull { child ->
                val id = child.key ?: return@mapNotNull null
                parseMessage(child, id)
            }
        }
    }

    override suspend fun deleteMessage(roomId: String, messageId: String) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_MESSAGES).child(messageId).removeValue().await()
        }
    }

    override suspend fun deleteAllMessages(roomId: String) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_MESSAGES).removeValue().await()
        }
    }

    override suspend fun writeRoomSignal(roomId: String, signal: RemoteRoomSignal) {
        require(signal.type == RemoteRoomSignal.TYPE_PING)
        require(signal.fromPub.length in 1..RemoteRoomSignal.MAX_PUB_LENGTH)
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            val payload = mapOf(
                KEY_TYPE to signal.type,
                KEY_FROM_PUB to signal.fromPub,
                KEY_CREATED_AT to signal.createdAt,
            )
            roomRef(roomId).child(PATH_SIGNALS).child(signal.signalId).setValue(payload).await()
        }
    }

    override suspend fun writeReport(report: RemoteReport) {
        require(report.roomId.length in 1..RemoteReport.MAX_ROOM_ID_LENGTH)
        require(report.reporterPub.length in 1..RemoteReport.MAX_PUB_LENGTH)
        report.peerPub?.let { require(it.length in 1..RemoteReport.MAX_PUB_LENGTH) }
        report.reason?.let { require(it.length <= RemoteReport.MAX_REASON_LENGTH) }
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            val payload = buildMap<String, Any> {
                put(KEY_ROOM_ID, report.roomId)
                put(KEY_REPORTER_PUB, report.reporterPub)
                put(KEY_CREATED_AT, report.createdAt)
                report.peerPub?.let { put(KEY_PEER_PUB, it) }
                report.reason?.takeIf { it.isNotBlank() }?.let { put(KEY_REASON, it) }
            }
            database.reference.child(PATH_REPORTS).child(report.reportId).setValue(payload).await()
        }
    }

    override fun observeMessages(roomId: String): Flow<List<RemoteMailboxMessage>> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_MESSAGES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    parseMessage(child, id)
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setPresence(roomId: String, deviceId: String, online: Boolean) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            val ref = roomRef(roomId).child(PATH_PRESENCE).child(deviceId)
            val payload = mapOf(
                KEY_ONLINE to online,
                KEY_UPDATED_AT to System.currentTimeMillis(),
            )
            if (online) {
                ref.onDisconnect().setValue(
                    mapOf(KEY_ONLINE to false, KEY_UPDATED_AT to System.currentTimeMillis()),
                ).await()
            }
            ref.setValue(payload).await()
        }
    }

    override fun observePresence(roomId: String): Flow<List<RemotePresence>> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_PRESENCE)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val online = child.child(KEY_ONLINE).getValue(Boolean::class.java)
                        ?: return@mapNotNull null
                    val updatedAt = child.child(KEY_UPDATED_AT).getValue(Long::class.java) ?: 0L
                    RemotePresence(deviceId = id, online = online, updatedAt = updatedAt)
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setReadWatermark(roomId: String, deviceId: String, messageId: String) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_READ).child(deviceId).setValue(
                mapOf(
                    KEY_MESSAGE_ID to messageId,
                    KEY_UPDATED_AT to System.currentTimeMillis(),
                ),
            ).await()
        }
    }

    override fun observeReadWatermarks(roomId: String): Flow<List<RemoteReadWatermark>> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_READ)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val messageId = child.child(KEY_MESSAGE_ID).getValue(String::class.java)
                        ?: return@mapNotNull null
                    val updatedAt = child.child(KEY_UPDATED_AT).getValue(Long::class.java) ?: 0L
                    RemoteReadWatermark(deviceId = id, messageId = messageId, updatedAt = updatedAt)
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setTyping(roomId: String, deviceId: String, atMs: Long) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_TYPING).child(deviceId)
                .setValue(mapOf(KEY_AT to atMs)).await()
        }
    }

    override suspend fun clearTyping(roomId: String, deviceId: String) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_TYPING).child(deviceId).removeValue().await()
        }
    }

    override fun observeTyping(roomId: String): Flow<List<RemoteTyping>> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_TYPING)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val at = child.child(KEY_AT).getValue(Long::class.java) ?: return@mapNotNull null
                    RemoteTyping(deviceId = id, at = at)
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setReaction(
        roomId: String,
        messageId: String,
        deviceId: String,
        emoji: String,
    ) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_REACTIONS).child(messageId).child(deviceId).setValue(
                mapOf(
                    KEY_EMOJI to emoji,
                    KEY_AT to System.currentTimeMillis(),
                ),
            ).await()
        }
    }

    override suspend fun clearReaction(roomId: String, messageId: String, deviceId: String) {
        ensureAuthenticated()
        withContext(dispatchersProvider.io) {
            roomRef(roomId).child(PATH_REACTIONS).child(messageId).child(deviceId).removeValue().await()
        }
    }

    override fun observeReactions(roomId: String): Flow<List<RemoteReaction>> = callbackFlow {
        ensureAuthenticated()
        val ref = roomRef(roomId).child(PATH_REACTIONS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RemoteReaction>()
                snapshot.children.forEach { msgChild ->
                    val messageId = msgChild.key ?: return@forEach
                    msgChild.children.forEach { deviceChild ->
                        val deviceId = deviceChild.key ?: return@forEach
                        val emoji = deviceChild.child(KEY_EMOJI).getValue(String::class.java)
                            ?: return@forEach
                        val at = deviceChild.child(KEY_AT).getValue(Long::class.java) ?: 0L
                        list += RemoteReaction(
                            messageId = messageId,
                            deviceId = deviceId,
                            emoji = emoji,
                            at = at,
                        )
                    }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun parseMessage(snapshot: DataSnapshot, messageId: String): RemoteMailboxMessage? {
        if (!snapshot.exists()) return null
        return toRemoteMessage(snapshot, messageId)
    }

    private fun parseRoomMeta(snapshot: DataSnapshot): RemoteRoomMeta? {
        if (!snapshot.exists()) return null
        val createdAt = snapshot.child(KEY_CREATED_AT).getValue(Long::class.java) ?: return null
        val expiresAt = snapshot.child(KEY_EXPIRES_AT).getValue(Long::class.java) ?: return null
        val creatorPub = snapshot.child(KEY_CREATOR_PUB).getValue(String::class.java)
        val icebreaker = snapshot.child(KEY_ICEBREAKER).getValue(String::class.java)
        val hostPro = snapshot.child(KEY_HOST_PRO).getValue(Boolean::class.java) ?: false
        val activatedAt = snapshot.child(KEY_ACTIVATED_AT).getValue(Long::class.java)
        return RemoteRoomMeta(
            createdAt = createdAt,
            expiresAt = expiresAt,
            creatorPub = creatorPub,
            icebreaker = icebreaker,
            hostPro = hostPro,
            activatedAt = activatedAt,
        )
    }

    private fun toRemoteMessage(snapshot: DataSnapshot, messageId: String): RemoteMailboxMessage? {
        val ciphertext = snapshot.child(KEY_CIPHERTEXT).getValue(String::class.java)
        val senderPub = snapshot.child(KEY_SENDER_PUB).getValue(String::class.java)
        val createdAt = snapshot.child(KEY_CREATED_AT).getValue(Long::class.java)
        val expiresAt = snapshot.child(KEY_EXPIRES_AT).getValue(Long::class.java)
        return when {
            ciphertext == null -> null
            senderPub == null -> null
            createdAt == null -> null
            expiresAt == null -> null
            else -> RemoteMailboxMessage(
                messageId = messageId,
                ciphertext = ciphertext,
                senderPub = senderPub,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }
    }

    private fun roomRef(roomId: String) = database.reference.child(PATH_ROOMS).child(roomId)

    private companion object {
        const val PATH_ROOMS = "rooms"
        const val PATH_META = "meta"
        const val PATH_MESSAGES = "messages"
        const val PATH_REPORTS = "reports"
        const val PATH_PRESENCE = "presence"
        const val PATH_READ = "read"
        const val PATH_TYPING = "typing"
        const val PATH_REACTIONS = "reactions"
        const val PATH_SIGNALS = "signals"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_HOST_PRO = "hostPro"
        const val KEY_ACTIVATED_AT = "activatedAt"
        const val KEY_CREATOR_PUB = "creatorPub"
        const val KEY_ICEBREAKER = "icebreaker"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_SENDER_PUB = "senderPub"
        const val KEY_ROOM_ID = "roomId"
        const val KEY_REPORTER_PUB = "reporterPub"
        const val KEY_PEER_PUB = "peerPub"
        const val KEY_REASON = "reason"
        const val KEY_ONLINE = "online"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_AT = "at"
        const val KEY_EMOJI = "emoji"
        const val KEY_TYPE = "type"
        const val KEY_FROM_PUB = "fromPub"
    }
}
