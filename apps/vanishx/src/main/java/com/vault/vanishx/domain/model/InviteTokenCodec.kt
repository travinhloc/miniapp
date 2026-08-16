@file:Suppress("ReturnCount", "ComplexMethod", "ComplexCondition")

package com.vault.vanishx.domain.model

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Opaque invite token (story 14.1): Base64URL of compact JSON
 * `{"v":1,"r":"<roomId>","k":"<roomKey>","e":<expiresAtMs>}`.
 */
object InviteTokenCodec {
    const val VERSION = 1
    const val DISPLAY_PREFIX_LEN = 8

    fun encode(invite: RoomInvite): String {
        val expires = invite.expiresAt?.takeIf { it > 0L } ?: 0L
        val json = buildString {
            append("{\"v\":")
            append(VERSION)
            append(",\"r\":\"")
            append(escapeJson(invite.roomId))
            append("\",\"k\":\"")
            append(escapeJson(invite.roomKey))
            append("\",\"e\":")
            append(expires)
            append('}')
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(token: String): RoomInvite? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return null
        val json = runCatching {
            String(Base64.getUrlDecoder().decode(trimmed), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        return parsePayload(json)
    }

    fun displayPrefix(token: String): String = token.take(DISPLAY_PREFIX_LEN)

    private fun parsePayload(json: String): RoomInvite? {
        val version = readNumber(json, "v")?.toInt() ?: return null
        if (version != VERSION) return null
        val roomId = readString(json, "r")?.takeIf { it.isNotBlank() } ?: return null
        val roomKey = readString(json, "k")?.takeIf { it.isNotBlank() } ?: return null
        val expires = readNumber(json, "e") ?: return null
        return RoomInvite(
            roomId = roomId,
            roomKey = roomKey,
            expiresAt = expires.takeIf { it > 0L },
        )
    }

    private fun readString(json: String, key: String): String? {
        val needle = "\"$key\":\""
        val start = json.indexOf(needle)
        if (start < 0) return null
        var i = start + needle.length
        val out = StringBuilder()
        while (i < json.length) {
            val c = json[i]
            when {
                c == '\\' && i + 1 < json.length -> {
                    out.append(json[i + 1])
                    i += 2
                }
                c == '"' -> return out.toString()
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return null
    }

    private fun readNumber(json: String, key: String): Long? {
        val needle = "\"$key\":"
        val start = json.indexOf(needle)
        if (start < 0) return null
        var i = start + needle.length
        while (i < json.length && json[i].isWhitespace()) i++
        val begin = i
        if (i < json.length && json[i] == '-') i++
        while (i < json.length && json[i].isDigit()) i++
        if (i == begin || (json[begin] == '-' && i == begin + 1)) return null
        return json.substring(begin, i).toLongOrNull()
    }

    private fun escapeJson(value: String): String =
        buildString(value.length) {
            value.forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    else -> append(c)
                }
            }
        }
}
