package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

/** Pulls RTDB room meta (clock / activate / hostPro) into the local room row. */
class RefreshRoomMetaUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val remote: MailboxRemoteDataSource,
) {
    suspend operator fun invoke(roomId: String): MailboxRoom? {
        val local = mailboxRepository.getRoom(roomId) ?: return null
        val meta = runCatching { remote.readRoomMeta(roomId) }.getOrNull() ?: return local
        val updated = local.copy(
            expiresAt = meta.expiresAt,
            hostPro = meta.hostPro,
            activatedAt = meta.activatedAt ?: local.activatedAt,
            icebreaker = meta.icebreaker ?: local.icebreaker,
            peerPub = local.peerPub ?: meta.creatorPub,
            status = local.copy(
                expiresAt = meta.expiresAt,
                hostPro = meta.hostPro,
            ).resolvedStatus(),
        )
        if (updated != local) {
            mailboxRepository.upsertRoom(updated)
        }
        return updated
    }
}
