package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

data class BlockPeerResult(
    val roomId: String,
    val peerPub: String,
)

class BlockPeerUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val blockRepository: BlockRepository,
    private val roomPushTopics: RoomPushTopics,
) {
    suspend operator fun invoke(roomId: String): BlockPeerResult {
        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        val peerPub = room.peerPub
            ?: error("Peer identity not known yet. Wait for a message or room meta.")
        require(peerPub.isNotBlank()) { "Peer identity not known yet" }

        blockRepository.block(peerPub)
        mailboxRepository.deleteMessagesForRoom(roomId)
        mailboxRepository.upsertRoom(
            room.copy(
                status = MailboxRoom.STATUS_LEFT,
                peerPub = peerPub,
            ),
        )
        roomPushTopics.unsubscribe(roomId)

        return BlockPeerResult(roomId = roomId, peerPub = peerPub)
    }
}
