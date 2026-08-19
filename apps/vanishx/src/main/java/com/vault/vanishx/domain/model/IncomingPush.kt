@file:Suppress("ReturnCount")

package com.vault.vanishx.domain.model

enum class IncomingPushType {
    MESSAGE,
    PING,
}

data class IncomingPush(
    val roomId: String,
    val type: IncomingPushType,
    val senderPub: String? = null,
)

data class IncomingPushContext(
    val push: IncomingPush,
    val room: MailboxRoom?,
    val myPub: String?,
    val foregroundRoomId: String?,
    val senderBlocked: Boolean,
    val nowMs: Long = System.currentTimeMillis(),
)

object IncomingPushParser {
    fun parse(data: Map<String, String>): IncomingPush? {
        val roomId = data[KEY_ROOM_ID]?.takeIf { it.isNotBlank() } ?: return null
        val typeRaw = data[KEY_TYPE]?.takeIf { it.isNotBlank() } ?: TYPE_MESSAGE
        val type = when (typeRaw) {
            TYPE_MESSAGE -> IncomingPushType.MESSAGE
            TYPE_PING -> IncomingPushType.PING
            else -> return null
        }
        val senderPub = data[KEY_SENDER_PUB]?.takeIf { it.isNotBlank() }
            ?: data[KEY_FROM_PUB]?.takeIf { it.isNotBlank() }
        return IncomingPush(roomId = roomId, type = type, senderPub = senderPub)
    }

    const val KEY_ROOM_ID = "roomId"
    const val KEY_TYPE = "type"
    const val KEY_SENDER_PUB = "senderPub"
    const val KEY_FROM_PUB = "fromPub"
    const val TYPE_MESSAGE = "message"
    const val TYPE_PING = "ping"
}

object IncomingPushPolicy {
    fun shouldNotify(context: IncomingPushContext): Boolean {
        val room = context.room ?: return false
        if (room.status == MailboxRoom.STATUS_LEFT) return false
        if (room.resolvedStatus(context.nowMs) == MailboxRoom.STATUS_EXPIRED) return false
        if (room.muted) return false
        if (context.senderBlocked) return false
        val sender = context.push.senderPub
        if (!sender.isNullOrBlank() && sender == context.myPub) return false
        if (context.foregroundRoomId == room.id) return false
        return true
    }
}
