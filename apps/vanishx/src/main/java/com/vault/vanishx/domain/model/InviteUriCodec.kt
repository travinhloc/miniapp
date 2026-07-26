package com.vault.vanishx.domain.model

/**
 * Invite format (story 2.2):
 * `vanishx://r/{roomId}?k={roomKey}&e={expiresAtEpochMs}`
 *
 * `e` is optional but included on create so join can show expiry offline.
 * Room key travels in the query string (MVP UX vs security tradeoff — see CRYPTO.md).
 */
object InviteUriCodec {
    const val SCHEME = "vanishx"
    const val HOST = "r"
    private const val QUERY_KEY = "k"
    private const val QUERY_EXPIRES = "e"
    private const val PREFIX = "$SCHEME://$HOST/"

    fun format(invite: RoomInvite): String {
        val base = "$PREFIX${invite.roomId}?$QUERY_KEY=${invite.roomKey}"
        return if (invite.expiresAt != null && invite.expiresAt > 0L) {
            "$base&$QUERY_EXPIRES=${invite.expiresAt}"
        } else {
            base
        }
    }

    fun parse(raw: String): RoomInvite? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return parseNormalized(normalize(trimmed))
    }

    private fun normalize(trimmed: String): String =
        when {
            trimmed.startsWith(PREFIX) -> trimmed
            trimmed.startsWith("$SCHEME://") -> trimmed
            else -> "$PREFIX$trimmed"
        }

    private fun parseNormalized(normalized: String): RoomInvite? {
        if (!normalized.startsWith(PREFIX)) return null

        val afterHost = normalized.removePrefix(PREFIX)
        val pathAndQuery = afterHost.split("?", limit = 2)
        val roomId = pathAndQuery[0].substringBefore('/').takeIf { it.isNotBlank() }
        val params = parseQuery(pathAndQuery.getOrNull(1).orEmpty())
        val roomKey = params[QUERY_KEY]?.takeIf { it.isNotBlank() }
        val expiresAt = params[QUERY_EXPIRES]?.toLongOrNull()?.takeIf { it > 0L }

        return if (roomId != null && roomKey != null) {
            RoomInvite(roomId = roomId, roomKey = roomKey, expiresAt = expiresAt)
        } else {
            null
        }
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2 && kv[0].isNotBlank()) kv[0] to kv[1] else null
            }
            .toMap()
}
