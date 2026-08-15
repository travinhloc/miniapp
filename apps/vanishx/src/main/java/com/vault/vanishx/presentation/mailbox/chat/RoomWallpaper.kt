@file:Suppress("MagicNumber", "ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

/**
 * Room wallpaper: either a local image path or a solid color preset (`color:#AARRGGBB` / `#RRGGBB`).
 */
internal object RoomWallpaper {
    const val COLOR_PREFIX = "color:"

    val presets: List<Pair<String, Long>> = listOf(
        "Night" to 0xFF0D1117L,
        "Slate" to 0xFF161B22L,
        "Ink" to 0xFF1A1F2EL,
        "Forest" to 0xFF0F1F1AL,
        "Wine" to 0xFF1F1218L,
        "Sand" to 0xFFE8E0D5L,
        "Mist" to 0xFFD7DEE8L,
        "Paper" to 0xFFF2F0EAL,
    )

    fun colorToken(argb: Long): String {
        val hex = (argb and 0xFFFFFFFFL).toString(16).padStart(8, '0').uppercase()
        return "$COLOR_PREFIX#$hex"
    }

    fun parseColorArgb(token: String?): Long? {
        if (token.isNullOrBlank() || !token.startsWith(COLOR_PREFIX)) return null
        val hex = token.removePrefix(COLOR_PREFIX).removePrefix("#")
        return hex.toLongOrNull(16)
    }

    fun isBright(tokenOrPath: String?): Boolean {
        if (tokenOrPath.isNullOrBlank()) return false
        val argb = parseColorArgb(tokenOrPath) ?: return true // gallery image → contrast scrim
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return lum >= 0.55
    }
}
