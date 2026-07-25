package com.vault.vanishx.data.local.db

import com.vault.vanishx.domain.model.MailboxRoom

fun MailboxRoomEntity.toDomain(): MailboxRoom = MailboxRoom(
    id = id,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    status = status,
)

fun MailboxRoom.toEntity(): MailboxRoomEntity = MailboxRoomEntity(
    id = id,
    createdAt = createdAt,
    expiresAt = expiresAt,
    title = title,
    status = status,
)
