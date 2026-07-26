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
@Suppress("TooManyFunctions")
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
                meta.creatorPub?.let { put(KEY_CREATOR_PUB, it) }
            }
            roomRef(roomId).child(PATH_META).setValue(payload).await()
        }
    }

    override suspend fun readRoomMeta(roomId: String): RemoteRoomMeta? {
        ensureAuthenticated()
        return withContext(dispatchersProvider.io) {
            val snapshot = roomRef(roomId).child(PATH_META).get().await()
            if (!snapshot.exists()) return@withContext null
            val createdAt = snapshot.child(KEY_CREATED_AT).getValue(Long::class.java) ?: return@withContext null
            val expiresAt = snapshot.child(KEY_EXPIRES_AT).getValue(Long::class.java) ?: return@withContext null
            val creatorPub = snapshot.child(KEY_CREATOR_PUB).getValue(String::class.java)
            RemoteRoomMeta(
                createdAt = createdAt,
                expiresAt = expiresAt,
                creatorPub = creatorPub,
            )
        }
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

    private fun parseMessage(snapshot: DataSnapshot, messageId: String): RemoteMailboxMessage? {
        if (!snapshot.exists()) return null
        return toRemoteMessage(snapshot, messageId)
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
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_CREATOR_PUB = "creatorPub"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_SENDER_PUB = "senderPub"
    }
}
