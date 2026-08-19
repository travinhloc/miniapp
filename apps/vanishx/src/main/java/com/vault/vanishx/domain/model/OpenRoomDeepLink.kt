package com.vault.vanishx.domain.model

/** FCM tray tap: `vanishx://open/{roomId}` (Epic 15 — not invite `vanishx://r/`). */
object OpenRoomDeepLink {
    const val SCHEME = "vanishx"
    const val HOST = "open"

    fun roomIdFrom(uri: String?): String? {
        val prefix = "$SCHEME://$HOST/"
        val rest = uri?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix) ?: return null
        return rest.substringBefore('?').substringBefore('#').takeIf { it.isNotBlank() }
    }
}
