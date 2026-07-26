package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import timber.log.Timber
import javax.inject.Inject

data class PurgeExpiredRoomResult(
    val roomId: String,
    val localDeleted: Int,
    val remotePurged: Boolean,
)

/**
 * Marks room expired, wipes local plaintext for the room, and removes remote mailbox messages
 * without decrypting. Room meta is left for invite discovery / status.
 */
class PurgeExpiredRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
    private val roomPushTopics: RoomPushTopics,
) {
    suspend operator fun invoke(roomId: String): PurgeExpiredRoomResult {
        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        mailboxRepository.upsertRoom(room.copy(status = MailboxRoom.STATUS_EXPIRED))

        val localDeleted = mailboxRepository.deleteMessagesForRoom(roomId)
        val remotePurged = runCatching {
            remote.deleteAllMessages(roomId)
        }.onFailure { e ->
            Timber.w(e, "Remote purge failed for room %s", roomId)
        }.isSuccess

        roomPushTopics.unsubscribe(roomId)

        return PurgeExpiredRoomResult(
            roomId = roomId,
            localDeleted = localDeleted,
            remotePurged = remotePurged,
        )
    }
}
