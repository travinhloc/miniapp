package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

/** Local display nickname (tên gợi nhớ) — Epic 12.3. */
class RenameRoomUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
) {
    suspend operator fun invoke(roomId: String, nickname: String): MailboxRoom {
        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        val trimmed = nickname.trim()
        require(trimmed.isNotEmpty()) { "Room title required" }
        require(trimmed.length <= MAX_TITLE_LEN) { "Room title too long" }
        val updated = room.copy(nickname = trimmed)
        mailboxRepository.upsertRoom(updated)
        return updated
    }

    companion object {
        const val MAX_TITLE_LEN = 40
    }
}
