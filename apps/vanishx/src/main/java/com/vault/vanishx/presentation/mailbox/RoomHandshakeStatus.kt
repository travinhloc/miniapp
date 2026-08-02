package com.vault.vanishx.presentation.mailbox

import com.vault.vanishx.domain.model.MailboxRoom

/**
 * Handshake status shown in the chat header/banner (story 7.7).
 *
 * Waiting = we don't know the peer's public key yet, i.e. no accepted message has been
 * exchanged (host waiting for the guest to accept, or guest hasn't heard back yet).
 * Live = peer accepted and messages sync through the mailbox — never phrased as P2P.
 */
enum class RoomHandshakeStatus {
    WAITING,
    LIVE,
    NONE,
}

fun handshakeStatus(room: MailboxRoom?, isExpired: Boolean): RoomHandshakeStatus = when {
    room == null || isExpired -> RoomHandshakeStatus.NONE
    room.status != MailboxRoom.STATUS_ACTIVE -> RoomHandshakeStatus.NONE
    room.peerPub.isNullOrBlank() -> RoomHandshakeStatus.WAITING
    else -> RoomHandshakeStatus.LIVE
}
