package com.vault.vanishx.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "rooms")
data class MailboxRoomEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val expiresAt: Long,
    val title: String?,
    val status: String,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val body: String,
    val sentAt: Long,
    val expiresAt: Long,
    val direction: String,
)
