@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "LongMethod", "TooManyFunctions")

package com.vault.vanishx.domain.model

/**
 * Encrypted payload plaintext format.
 * - Legacy / plain: raw UTF-8 (no sensitive, no reply).
 * - v1: `{"v":1,"s":bool,"t":"...","r":"replyId?"}`
 * - v2 media: `{"v":2,"k":"image|file|video","m":"mime","b":bytes,"a":"attId",...}`
 *   (no caption — E11-5; no sensitive/reply — E11-6)
 */
object MessagePlaintextCodec {

    data class Decoded(
        val text: String,
        val sensitive: Boolean,
        val replyToId: String? = null,
        val attachment: AttachmentMeta? = null,
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

    fun encodeAttachment(meta: AttachmentMeta): String {
        require(meta.attId.isNotBlank()) { "attId required" }
        require(meta.mime.isNotBlank()) { "mime required" }
        require(meta.bytes > 0L) { "bytes must be > 0" }
        return buildString {
            append("{\"v\":2,\"k\":")
            append(quote(meta.kind))
            append(",\"m\":")
            append(quote(meta.mime))
            append(",\"b\":")
            append(meta.bytes)
            append(",\"a\":")
            append(quote(meta.attId))
            meta.width?.let {
                append(",\"w\":")
                append(it)
            }
            meta.height?.let {
                append(",\"h\":")
                append(it)
            }
            meta.fileName?.takeIf { it.isNotBlank() }?.let {
                append(",\"fn\":")
                append(quote(it))
            }
            meta.albumId?.takeIf { it.isNotBlank() }?.let {
                append(",\"g\":")
                append(quote(it))
            }
            append('}')
        }
    }

    fun decode(raw: String): Decoded {
        when {
            raw.startsWith(ENVELOPE_V2_PREFIX) -> return decodeV2(raw)
            raw.startsWith(ENVELOPE_V1_PREFIX) -> return decodeV1(raw)
            else -> return Decoded(text = raw, sensitive = false, replyToId = null)
        }
    }

    private fun decodeV1(raw: String): Decoded {
        val sensitive = when {
            raw.contains("\"s\":true") -> true
            raw.contains("\"s\":false") -> false
            else -> false
        }
        val text = readQuotedField(raw, "\"t\":") ?: return Decoded(text = raw, sensitive = false)
        val replyToId = readQuotedField(raw, "\"r\":")
        return Decoded(text = text, sensitive = sensitive, replyToId = replyToId)
    }

    private fun decodeV2(raw: String): Decoded {
        val kind = readQuotedField(raw, "\"k\":") ?: return Decoded(text = raw, sensitive = false)
        val mime = readQuotedField(raw, "\"m\":") ?: return Decoded(text = raw, sensitive = false)
        val attId = readQuotedField(raw, "\"a\":") ?: return Decoded(text = raw, sensitive = false)
        val bytes = readLongField(raw, "\"b\":") ?: return Decoded(text = raw, sensitive = false)
        val width = readIntField(raw, "\"w\":")
        val height = readIntField(raw, "\"h\":")
        val fileName = readQuotedField(raw, "\"fn\":")
        val albumId = readQuotedField(raw, "\"g\":")
        return Decoded(
            text = "",
            sensitive = false,
            replyToId = null,
            attachment = AttachmentMeta(
                kind = kind,
                mime = mime,
                bytes = bytes,
                attId = attId,
                width = width,
                height = height,
                fileName = fileName,
                albumId = albumId,
            ),
        )
    }

    private fun readQuotedField(raw: String, key: String): String? {
        val index = raw.indexOf(key)
        if (index < 0) return null
        return unquote(raw, index + key.length)
    }

    private fun readLongField(raw: String, key: String): Long? {
        val index = raw.indexOf(key)
        if (index < 0) return null
        var i = index + key.length
        while (i < raw.length && raw[i].isWhitespace()) i++
        val start = i
        while (i < raw.length && (raw[i].isDigit())) i++
        if (start == i) return null
        return raw.substring(start, i).toLongOrNull()
    }

    private fun readIntField(raw: String, key: String): Int? = readLongField(raw, key)?.toInt()

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

    private const val ENVELOPE_V1_PREFIX = "{\"v\":1,"
    private const val ENVELOPE_V2_PREFIX = "{\"v\":2,"
}
