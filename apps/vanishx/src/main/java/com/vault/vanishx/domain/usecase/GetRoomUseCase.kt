package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

class GetRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
) {
    suspend operator fun invoke(roomId: String): MailboxRoom? {
        val room = mailboxRepository.getRoom(roomId) ?: return null
        val resolved = room.copy(status = room.resolvedStatus())
        if (resolved.status != room.status) {
            mailboxRepository.upsertRoom(resolved)
        }
        return resolved
    }
}
