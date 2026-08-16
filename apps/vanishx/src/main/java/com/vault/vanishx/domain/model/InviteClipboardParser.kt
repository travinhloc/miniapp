@file:Suppress("ReturnCount")

package com.vault.vanishx.domain.model

/**
 * Clipboard payload from the web landing (story 14.3):
 * `VANISHX_INVITE:JOIN_ROOM:{opaqueToken}:{expirationMs}`
 */
object InviteClipboardParser {
    const val PREFIX = "VANISHX_INVITE"
    const val ACTION_JOIN_ROOM = "JOIN_ROOM"

    sealed interface Result {
        data object Ignore : Result
        data class Discard(val reason: String) : Result
        data class Valid(val invite: RoomInvite) : Result
    }

    fun parse(raw: String?, nowMs: Long): Result {
        val text = raw?.trim().orEmpty()
        val prefix = "$PREFIX:"
        if (!text.startsWith(prefix)) return Result.Ignore
        val rest = text.removePrefix(prefix)
        val lastColon = rest.lastIndexOf(':')
        if (lastColon <= 0) return Result.Discard("malformed")
        val expMs = rest.substring(lastColon + 1).toLongOrNull()
            ?: return Result.Discard("malformed")
        val actionAndToken = rest.substring(0, lastColon)
        val actionColon = actionAndToken.indexOf(':')
        if (actionColon <= 0) return Result.Discard("malformed")
        val action = actionAndToken.substring(0, actionColon)
        val token = actionAndToken.substring(actionColon + 1).trim()
        if (action != ACTION_JOIN_ROOM) return Result.Discard("unknown_action")
        if (nowMs >= expMs) return Result.Discard("expired")
        if (token.isEmpty()) return Result.Discard("malformed")
        val invite = InviteTokenCodec.decode(token) ?: return Result.Discard("bad_token")
        return Result.Valid(invite)
    }
}
