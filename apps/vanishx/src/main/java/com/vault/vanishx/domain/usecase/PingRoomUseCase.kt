package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

data class PingRoomResult(
    val roomId: String,
    val remoteMetaPresent: Boolean,
)

/**
 * "Gọi lại phòng" — checks remote meta still exists; does not create a new room.
 */
class PingRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
) {
    suspend operator fun invoke(roomId: String): PingRoomResult {
        val room = mailboxRepository.getRoom(roomId) ?: error("Room not found")
        require(room.roomKey.isNotBlank())
        val meta = runCatching { remote.readRoomMeta(roomId) }.getOrNull()
        return PingRoomResult(
            roomId = roomId,
            remoteMetaPresent = meta != null,
        )
    }
}
