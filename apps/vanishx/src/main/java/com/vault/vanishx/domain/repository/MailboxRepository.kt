package com.vault.vanishx.domain.repository

import com.vault.vanishx.domain.model.MailboxRoom

/**
 * App-local repository contract. Firebase mailbox arrives in story 2.1+.
 */
interface MailboxRepository {
    suspend fun getActiveRooms(): List<MailboxRoom>
    suspend fun upsertRoom(room: MailboxRoom)
}
