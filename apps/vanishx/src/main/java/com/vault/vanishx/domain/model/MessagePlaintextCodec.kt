package com.vault.vanishx.domain.model

/**
 * Encrypted payload plaintext format.
 * - Legacy / normal: raw UTF-8 text (sensitive = false).
 * - Sensitive: `{"v":1,"s":true,"t":"<escaped text>"}` so peers learn the flag after decrypt.
 */
object MessagePlaintextCodec {

    data class Decoded(
        val text: String,
        val sensitive: Boolean,
    )

    fun encode(text: String, sensitive: Boolean): String {
        if (!sensitive) return text
        return """{"v":1,"s":true,"t":${quote(text)}}"""
    }

    fun decode(raw: String): Decoded {
        if (!raw.startsWith(SENSITIVE_PREFIX)) {
            return Decoded(text = raw, sensitive = false)
        }
        val textKey = "\"t\":"
        val textIndex = raw.indexOf(textKey)
        if (textIndex < 0) return Decoded(text = raw, sensitive = false)
        val textStart = textIndex + textKey.length
        val text = unquote(raw, textStart) ?: return Decoded(text = raw, sensitive = false)
        return Decoded(text = text, sensitive = true)
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

    private const val SENSITIVE_PREFIX = "{\"v\":1,"
}
