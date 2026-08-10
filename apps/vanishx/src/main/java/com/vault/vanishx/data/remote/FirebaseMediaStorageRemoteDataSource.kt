package com.vault.vanishx.data.remote

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase Storage only ever receives RoomBlobCipher ciphertext. */
@Singleton
class FirebaseMediaStorageRemoteDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) : MediaStorageRemoteDataSource {

    override suspend fun upload(
        roomId: String,
        messageId: String,
        attId: String,
        ciphertext: ByteArray,
    ) {
        val path = "rooms/$roomId/att/$messageId/$attId"
        try {
            ref(roomId, messageId, attId).putBytes(ciphertext).await()
        } catch (e: StorageException) {
            Timber.e(
                e,
                "Storage upload failed path=%s bucket=%s",
                path,
                storage.app.options.storageBucket,
            )
            throw e
        }
    }

    override suspend fun download(
        roomId: String,
        messageId: String,
        attId: String,
        maxBytes: Long,
    ): ByteArray = ref(roomId, messageId, attId).getBytes(maxBytes).await()

    override suspend fun delete(roomId: String, messageId: String, attId: String) {
        runCatching {
            ref(roomId, messageId, attId).delete().await()
        }.onFailure { e ->
            Timber.w(e, "Storage delete failed %s/%s/%s", roomId, messageId, attId)
        }
    }

    override suspend fun deleteRoomPrefix(roomId: String) {
        runCatching {
            deleteRecursively(storage.reference.child("rooms").child(roomId).child("att"))
        }.onFailure { e ->
            Timber.w(e, "Storage deleteRoomPrefix failed %s", roomId)
        }
    }

    private suspend fun deleteRecursively(ref: StorageReference) {
        val listed = ref.listAll().await()
        listed.items.forEach { item ->
            runCatching { item.delete().await() }
        }
        listed.prefixes.forEach { prefix ->
            deleteRecursively(prefix)
        }
    }

    private fun ref(roomId: String, messageId: String, attId: String): StorageReference =
        storage.reference
            .child("rooms")
            .child(roomId)
            .child("att")
            .child(messageId)
            .child(attId)
}
