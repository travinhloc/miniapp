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
        val local = mailboxRepository.getRoom(roomId)
        val meta = if (local != null) {
            runCatching { remote.readRoomMeta(roomId) }.getOrNull()
        } else {
            null
        }
        val result = when {
            local == null -> null
            meta == null -> local
            else -> {
                val updated = local.copy(
                    expiresAt = meta.expiresAt,
                    hostPro = meta.hostPro,
                    activatedAt = meta.activatedAt ?: local.activatedAt,
                    icebreaker = meta.icebreaker ?: local.icebreaker,
                    peerPub = resolvedPeerPub(local, meta.creatorPub),
                    status = local.copy(
                        expiresAt = meta.expiresAt,
                        hostPro = meta.hostPro,
                    ).resolvedStatus(),
                )
                if (updated != local) {
                    mailboxRepository.upsertRoom(updated)
                }
                updated
            }
        }
        return result
    }
}

private fun resolvedPeerPub(local: MailboxRoom, creatorPub: String?): String? {
    val stored = local.peerPub?.takeIf { it.isNotBlank() }
    return when (local.role) {
        MailboxRoom.ROLE_CREATOR -> stored?.takeUnless { it == creatorPub }
        else -> stored ?: creatorPub
    }
}
