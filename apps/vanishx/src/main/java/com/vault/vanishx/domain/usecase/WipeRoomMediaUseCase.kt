package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.media.LocalMediaStore
import com.vault.vanishx.data.remote.MediaStorageRemoteDataSource
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

class WipeRoomMediaUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val mediaStorage: MediaStorageRemoteDataSource,
    private val localMediaStore: LocalMediaStore,
) {
    /** Alias used by purge path. */
    suspend fun room(roomId: String, deleteRemote: Boolean = true) =
        wipeRoom(roomId, deleteRemote)

    suspend fun wipeRoom(roomId: String, deleteRemote: Boolean = true) {
        if (deleteRemote) {
            runCatching { mediaStorage.deleteRoomPrefix(roomId) }
        }
        localMediaStore.wipeRoom(roomId)
    }

    suspend fun wipeMessage(roomId: String, messageId: String, attId: String?) {
        if (!attId.isNullOrBlank()) {
            runCatching { mediaStorage.delete(roomId, messageId, attId) }
        }
        localMediaStore.wipeMessage(roomId, messageId)
    }

    /** Alias used by panic wipe. */
    fun localAll() = wipeAllLocal()

    fun wipeAllLocal() {
        localMediaStore.wipeAll()
    }

    suspend fun wipeAllRoomsRemoteAndLocal() {
        mailboxRepository.getAllRooms().forEach { room ->
            wipeRoom(room.id, deleteRemote = true)
        }
        wipeAllLocal()
    }
}
