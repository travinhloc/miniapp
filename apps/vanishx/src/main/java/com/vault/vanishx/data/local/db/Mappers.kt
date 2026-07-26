package com.vault.vanishx.data.local.db

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
