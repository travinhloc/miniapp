package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

/** Delete for me (E9-10 / 9.14) — local only, outbound v1. */
class DeleteLocalMessageUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
) {
    suspend operator fun invoke(roomId: String, messageId: String) {
        val existing = mailboxRepository.getMessage(messageId)
            ?: error("Message not found")
        require(existing.roomId == roomId) { "Message not in this room" }
        require(existing.direction == ChatMessage.DIRECTION_OUT) {
            "Only your own messages can be deleted locally in v1"
        }
        mailboxRepository.deleteMessage(messageId)
    }
}
