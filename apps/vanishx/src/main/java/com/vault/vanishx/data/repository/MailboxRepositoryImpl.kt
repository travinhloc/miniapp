package com.vault.vanishx.data.repository

import com.vault.vanishx.data.local.db.VanishxLocalDatabase
import com.vault.vanishx.data.local.db.toDomain
import com.vault.vanishx.data.local.db.toEntity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailboxRepositoryImpl @Inject constructor(
    private val localDatabase: VanishxLocalDatabase,
) : MailboxRepository {

    override suspend fun getActiveRooms(): List<MailboxRoom> =
        localDatabase.withDatabase { db ->
            db.mailboxRoomDao()
                .getByStatus(MailboxRoom.STATUS_ACTIVE)
                .map { it.toDomain() }
        }

    override suspend fun upsertRoom(room: MailboxRoom) {
        localDatabase.withDatabase { db ->
            db.mailboxRoomDao().upsert(room.toEntity())
        }
    }
}
