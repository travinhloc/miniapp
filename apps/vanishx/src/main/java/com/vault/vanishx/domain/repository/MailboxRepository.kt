package com.vault.vanishx.domain.repository

import com.vault.vanishx.domain.model.MailboxRoom

interface MailboxRepository {
    suspend fun getActiveRooms(): List<MailboxRoom>
    suspend fun getRoom(roomId: String): MailboxRoom?
    suspend fun upsertRoom(room: MailboxRoom)
}
