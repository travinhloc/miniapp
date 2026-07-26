package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import timber.log.Timber
import javax.inject.Inject

data class SyncActiveMailboxesResult(
    val activeCount: Int,
    val purgedCount: Int,
    val syncedCount: Int,
    val syncFailures: Int,
)

/**
 * Home / resume: re-resolve TTL for locally "active" rooms, purge expired, sync the rest.
 * Network failures on individual rooms do not throw.
 */
class SyncActiveMailboxesUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val syncRoomMailbox: SyncRoomMailboxUseCase,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
) {
    suspend operator fun invoke(): SyncActiveMailboxesResult {
        val rooms = mailboxRepository.getActiveRooms()
        val now = System.currentTimeMillis()
        var purged = 0
        var synced = 0
        var failures = 0

        for (room in rooms) {
            val status = room.resolvedStatus(now)
            if (status == MailboxRoom.STATUS_EXPIRED) {
                runCatching { purgeExpiredRoom(room.id) }
                    .onFailure { e -> Timber.w(e, "Purge on open failed for %s", room.id) }
                purged++
                continue
            }
            runCatching { syncRoomMailbox(room.id) }
                .onSuccess { synced++ }
                .onFailure { e ->
                    failures++
                    Timber.w(e, "Sync on open failed for %s", room.id)
                }
        }

        val stillActive = mailboxRepository.getActiveRooms()
            .count { it.resolvedStatus(now) == MailboxRoom.STATUS_ACTIVE }

        return SyncActiveMailboxesResult(
            activeCount = stillActive,
            purgedCount = purged,
            syncedCount = synced,
            syncFailures = failures,
        )
    }
}
