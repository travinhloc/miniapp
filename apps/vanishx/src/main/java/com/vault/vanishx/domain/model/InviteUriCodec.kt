@file:Suppress("TooManyFunctions", "ReturnCount")

package com.vault.vanishx.domain.model

import java.net.URI

/**
 * Invite links (story 14.1):
 * - Canonical: `https://{host}/join?token={opaque}`
 * - Short display: `{host}/j/{8}` — QR/copy still use canonical
 * - Fallback parse: `vanishx://r/{roomId}?k={roomKey}&e={expiresAt}`
 */
object InviteUriCodec {
    const val SCHEME = "vanishx"
    const val HOST = "r"
    private const val QUERY_KEY = "k"
    private const val QUERY_EXPIRES = "e"
    private const val PREFIX = "$SCHEME://$HOST/"
    private const val HTTPS = "https://"

    /** Flavor host; [com.vault.vanishx.MainApplication] sets this from BuildConfig. */
    @Volatile
    var httpsHost: String = "vanihx-staging.web.app"

    fun format(invite: RoomInvite, host: String = httpsHost): String {
        val token = InviteTokenCodec.encode(invite)
        return "$HTTPS$host/join?token=$token"
    }

    fun displayShort(invite: RoomInvite, host: String = httpsHost): String {
        val token = InviteTokenCodec.encode(invite)
        return "$host/j/${InviteTokenCodec.displayPrefix(token)}"
    }

    fun displayShort(raw: String, host: String = httpsHost): String? {
        val invite = parse(raw) ?: return null
        return displayShort(invite, host)
    }

    fun parse(raw: String): RoomInvite? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith(HTTPS, ignoreCase = true) -> parseHttps(trimmed)
            trimmed.startsWith("http://", ignoreCase = true) -> null
            else -> parseNormalized(normalize(trimmed))
        }
    }

    private fun normalize(trimmed: String): String =
        when {
            trimmed.startsWith(PREFIX) -> trimmed
            trimmed.startsWith("$SCHEME://") -> trimmed
            else -> "$PREFIX$trimmed"
        }

    private fun parseHttps(raw: String): RoomInvite? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        val query = uri.rawQuery.orEmpty()
        if (hasBannedHttpsQuery(query)) return null
        val path = uri.path.orEmpty().trimEnd('/')
        val token = when {
            path == "/join" -> queryValue(query, "token")
            path.startsWith("/j/") -> path.removePrefix("/j/").substringBefore('/')
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
        if (token.length <= InviteTokenCodec.DISPLAY_PREFIX_LEN) return null
        return InviteTokenCodec.decode(token)
    }

    private fun hasBannedHttpsQuery(query: String): Boolean =
        query.split("&").any { part ->
            val key = part.substringBefore('=').lowercase()
            key == QUERY_KEY || key == "roomid" || key == "roomkey"
        }

    private fun queryValue(query: String, name: String): String? =
        query.split("&")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2 && kv[0] == name) kv[1] else null
            }
            .firstOrNull()

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
