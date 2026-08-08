@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount")

package com.vault.vanishx.domain.model

/**
 * Encrypted payload plaintext format.
 * - Legacy / plain: raw UTF-8 (no sensitive, no reply).
 * - Envelope: `{"v":1,"s":bool,"t":"...","r":"replyId?"}` when sensitive and/or reply.
 */
object MessagePlaintextCodec {

    data class Decoded(
        val text: String,
        val sensitive: Boolean,
        val replyToId: String? = null,
    )

    fun encode(
        text: String,
        sensitive: Boolean,
        replyToId: String? = null,
    ): String {
        val reply = replyToId?.takeIf { it.isNotBlank() }
        if (!sensitive && reply == null) return text
        return buildString {
            append("{\"v\":1,\"s\":")
            append(sensitive)
            append(",\"t\":")
            append(quote(text))
            if (reply != null) {
                append(",\"r\":")
                append(quote(reply))
            }
            append('}')
        }
    }

    fun decode(raw: String): Decoded {
        if (!raw.startsWith(ENVELOPE_PREFIX)) {
            return Decoded(text = raw, sensitive = false, replyToId = null)
        }
        val sensitive = when {
            raw.contains("\"s\":true") -> true
            raw.contains("\"s\":false") -> false
            else -> false
        }
        val text = readQuotedField(raw, "\"t\":") ?: return Decoded(text = raw, sensitive = false)
        val replyToId = readQuotedField(raw, "\"r\":")
        return Decoded(text = text, sensitive = sensitive, replyToId = replyToId)
    }

    private fun readQuotedField(raw: String, key: String): String? {
        val index = raw.indexOf(key)
        if (index < 0) return null
        return unquote(raw, index + key.length)
    }

    private fun quote(value: String): String = buildString(value.length + 2) {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun unquote(raw: String, start: Int): String? {
        if (start >= raw.length || raw[start] != '"') return null
        val out = StringBuilder()
        var i = start + 1
        while (i < raw.length) {
            val ch = raw[i]
            when {
                ch == '\\' && i + 1 < raw.length -> {
                    when (raw[i + 1]) {
                        '\\' -> out.append('\\')
                        '"' -> out.append('"')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        else -> out.append(raw[i + 1])
                    }
                    i += 2
                }
                ch == '"' -> return out.toString()
                else -> {
                    out.append(ch)
                    i += 1
                }
            }
        }
        return null
    }

    private const val ENVELOPE_PREFIX = "{\"v\":1,"
}
