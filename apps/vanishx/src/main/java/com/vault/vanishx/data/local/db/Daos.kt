package com.vault.vanishx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MetaEntity)

    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): MetaEntity?
}

@Dao
interface MailboxRoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MailboxRoomEntity)

    @Query("SELECT * FROM rooms WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getByStatus(status: String): List<MailboxRoomEntity>

    @Query("SELECT * FROM rooms ORDER BY createdAt DESC")
    suspend fun getAll(): List<MailboxRoomEntity>

    @Query("SELECT * FROM rooms WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MailboxRoomEntity?

    @Query("UPDATE rooms SET lastReadMessageId = :messageId WHERE id = :roomId")
    suspend fun setLastReadMessageId(roomId: String, messageId: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MessageEntity)

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY sentAt ASC")
    suspend fun getByRoom(roomId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MessageEntity?

    @Query(
        "SELECT * FROM messages WHERE roomId = :roomId AND (expiresAt = 0 OR expiresAt > :nowMs) " +
            "ORDER BY sentAt DESC LIMIT 1",
    )
    suspend fun getLatestVisible(roomId: String, nowMs: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE expiresAt > 0 AND expiresAt <= :nowMs")
    suspend fun deleteExpired(nowMs: Long): Int

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun deleteByRoom(roomId: String): Int

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String): Int
}

@Dao
interface BlockedPeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockedPeerEntity)

    @Query("SELECT * FROM blocked_peers WHERE peerPub = :peerPub LIMIT 1")
    suspend fun get(peerPub: String): BlockedPeerEntity?

    @Query("SELECT * FROM blocked_peers ORDER BY blockedAt DESC")
    suspend fun listAll(): List<BlockedPeerEntity>

    @Query("SELECT peerPub FROM blocked_peers")
    suspend fun listPeerPubs(): List<String>

    @Query("DELETE FROM blocked_peers WHERE peerPub = :peerPub")
    suspend fun delete(peerPub: String)
}
