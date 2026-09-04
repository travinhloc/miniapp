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
    val roomKey: String,
    val createdAt: Long,
    val expiresAt: Long,
    val title: String?,
    val nickname: String? = null,
    val status: String,
    val role: String,
    val peerPub: String? = null,
    val icebreaker: String? = null,
    val hostPro: Boolean = false,
    val activatedAt: Long = 0L,
    val muted: Boolean = false,
    val favorite: Boolean = false,
    val avatarLocalPath: String? = null,
    val wallpaperLocalPath: String? = null,
    val lastReadMessageId: String? = null,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val body: String,
    val sentAt: Long,
    val expiresAt: Long,
    val direction: String,
    val recalled: Boolean = false,
    val sensitive: Boolean = false,
    val replyToId: String? = null,
    val mediaKind: String? = null,
    val mediaMime: String? = null,
    val mediaBytes: Long? = null,
    val mediaAttId: String? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val mediaFileName: String? = null,
    val mediaAlbumId: String? = null,
    val mediaLocalPath: String? = null,
    val mediaTransferStatus: String? = null,
)

@Entity(tableName = "blocked_peers")
data class BlockedPeerEntity(
    @PrimaryKey val peerPub: String,
    val blockedAt: Long,
)
