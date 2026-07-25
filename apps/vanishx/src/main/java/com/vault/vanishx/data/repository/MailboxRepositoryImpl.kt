package com.vault.vanishx.data.repository

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

class MailboxRepositoryImpl @Inject constructor() : MailboxRepository {

    override suspend fun getActiveRooms(): List<MailboxRoom> = emptyList()
}
