package com.vault.vanishx.data.local.db

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom

fun MailboxRoomEntity.toDomain(): MailboxRoom = MailboxRoom(
    id = id,
    roomKey = roomKey,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    status = status,
    role = role,
)

fun MailboxRoom.toEntity(): MailboxRoomEntity = MailboxRoomEntity(
    id = id,
    roomKey = roomKey,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    status = status,
    role = role,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    roomId = roomId,
    body = body,
    sentAt = sentAt,
    expiresAt = expiresAt,
    direction = direction,
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    roomId = roomId,
    body = body,
    sentAt = sentAt,
    expiresAt = expiresAt,
    direction = direction,
)
