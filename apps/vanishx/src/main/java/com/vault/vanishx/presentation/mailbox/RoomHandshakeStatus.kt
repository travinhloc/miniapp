package com.vault.vanishx.presentation.mailbox

import com.vault.vanishx.domain.model.MailboxRoom

/**
 * Handshake status shown in the chat header/banner (story 7.7 / Free-Pro activate).
 *
 * Waiting = guest has not entered yet (no [MailboxRoom.activatedAt], no peer key).
 * Live = guest entered (activate) and/or peer key known — mailbox sync, never P2P wording.
 */
enum class RoomHandshakeStatus {
    WAITING,
    LIVE,
    NONE,
}

fun handshakeStatus(room: MailboxRoom?, isExpired: Boolean): RoomHandshakeStatus = when {
    room == null || isExpired -> RoomHandshakeStatus.NONE
    room.status != MailboxRoom.STATUS_ACTIVE -> RoomHandshakeStatus.NONE
    room.activatedAt > 0L || !room.peerPub.isNullOrBlank() -> RoomHandshakeStatus.LIVE
    else -> RoomHandshakeStatus.WAITING
}
