package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteReport
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import java.util.UUID
import javax.inject.Inject

data class ReportRoomResult(
    val reportId: String,
)

class ReportRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
) {
    suspend operator fun invoke(roomId: String, reason: String?): ReportRoomResult {
        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        val trimmedReason = reason?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmedReason != null) {
            require(trimmedReason.length <= RemoteReport.MAX_REASON_LENGTH) {
                "Reason too long"
            }
        }
        val reporterPub = identityRepository.ensureIdentity().publicKeyBase64
        val reportId = UUID.randomUUID().toString()
        remote.writeReport(
            RemoteReport(
                reportId = reportId,
                roomId = room.id,
                reporterPub = reporterPub,
                peerPub = room.peerPub,
                reason = trimmedReason,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return ReportRoomResult(reportId = reportId)
    }
}
