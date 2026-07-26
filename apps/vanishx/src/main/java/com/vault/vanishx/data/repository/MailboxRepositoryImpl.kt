package com.vault.vanishx.data.repository

import com.vault.vanishx.data.local.db.VanishxLocalDatabase
import com.vault.vanishx.data.local.db.toDomain
import com.vault.vanishx.data.local.db.toEntity
import com.vault.vanishx.domain.model.ChatMessage
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

    override suspend fun getRoom(roomId: String): MailboxRoom? =
        localDatabase.withDatabase { db ->
            db.mailboxRoomDao().getById(roomId)?.toDomain()
        }

    override suspend fun upsertRoom(room: MailboxRoom) {
        localDatabase.withDatabase { db ->
            db.mailboxRoomDao().upsert(room.toEntity())
        }
    }

    override suspend fun getMessages(roomId: String): List<ChatMessage> =
        localDatabase.withDatabase { db ->
            db.messageDao().getByRoom(roomId).map { it.toDomain() }
        }

    override suspend fun getMessage(messageId: String): ChatMessage? =
        localDatabase.withDatabase { db ->
            db.messageDao().getById(messageId)?.toDomain()
        }

    override suspend fun upsertMessage(message: ChatMessage) {
        localDatabase.withDatabase { db ->
            db.messageDao().upsert(message.toEntity())
        }
    }

    override suspend fun deleteExpiredMessages(nowMs: Long): Int =
        localDatabase.withDatabase { db ->
            db.messageDao().deleteExpired(nowMs)
        }

    override suspend fun deleteMessagesForRoom(roomId: String): Int =
        localDatabase.withDatabase { db ->
            db.messageDao().deleteByRoom(roomId)
        }
}
