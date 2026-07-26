package com.vault.vanishx.data.local.db

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom

fun MailboxRoomEntity.toDomain(): MailboxRoom = MailboxRoom(
    id = id,
    roomKey = roomKey,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    nickname = nickname,
    status = status,
    role = role,
    peerPub = peerPub,
)

fun MailboxRoom.toEntity(): MailboxRoomEntity = MailboxRoomEntity(
    id = id,
    roomKey = roomKey,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    nickname = nickname,
    status = status,
    role = role,
    peerPub = peerPub,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    roomId = roomId,
    body = body,
    sentAt = sentAt,
    expiresAt = expiresAt,
    direction = direction,
    recalled = recalled,
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    roomId = roomId,
    body = body,
    sentAt = sentAt,
    expiresAt = expiresAt,
    direction = direction,
    recalled = recalled,
)
