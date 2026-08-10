package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.MediaStorageRemoteDataSource
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.RecallPolicy
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import timber.log.Timber
import javax.inject.Inject

data class RecallMessageResult(
    val message: ChatMessage,
    val remoteRemoved: Boolean,
)

class RecallRoomMessageUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val proEntitlement: ProEntitlementRepository,
    private val remote: MailboxRemoteDataSource,
    private val mediaRemote: MediaStorageRemoteDataSource,
) {
    suspend operator fun invoke(
        roomId: String,
        messageId: String,
        nowMs: Long = System.currentTimeMillis(),
    ): RecallMessageResult {
        val existing = mailboxRepository.getMessage(messageId)
            ?: error("Message not found")
        require(existing.roomId == roomId) { "Message not in this room" }
        require(existing.direction == ChatMessage.DIRECTION_OUT) {
            "Only your own messages can be recalled"
        }
        if (existing.recalled) {
            return RecallMessageResult(message = existing, remoteRemoved = true)
        }

        val isPro = proEntitlement.isProNow()
        check(RecallPolicy.canRecallOutbound(existing.sentAt, isPro, nowMs)) {
            "Pro required to recall messages older than 24h"
        }

        val remoteRemoved = runCatching {
            remote.deleteMessage(roomId, messageId)
        }.onFailure { e ->
            Timber.w(e, "Remote recall delete failed for %s", messageId)
        }.isSuccess
        existing.mediaAttId?.let { attId ->
            runCatching { mediaRemote.delete(roomId, messageId, attId) }
                .onFailure { Timber.w(it, "Media recall delete failed for %s", messageId) }
        }

        val recalled = existing.copy(
            body = "",
            recalled = true,
        )
        mailboxRepository.upsertMessage(recalled)

        return RecallMessageResult(
            message = recalled,
            remoteRemoved = remoteRemoved,
        )
    }
}
