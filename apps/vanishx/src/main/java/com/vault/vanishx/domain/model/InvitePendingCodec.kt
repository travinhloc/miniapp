@file:Suppress("ReturnCount")

package com.vault.vanishx.domain.model

/**
 * Normalize any invite source (HTTPS, vanishx://, clipboard payload) to the canonical
 * HTTPS URI stored in [com.vault.vanishx.data.invite.PendingInviteStore] (story 14.4).
 */
object InvitePendingCodec {
    fun canonicalize(raw: String?, nowMs: Long = System.currentTimeMillis()): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        InviteUriCodec.parse(trimmed)?.let { return InviteUriCodec.format(it) }
        return when (val parsed = InviteClipboardParser.parse(trimmed, nowMs)) {
            is InviteClipboardParser.Result.Valid -> InviteUriCodec.format(parsed.invite)
            else -> null
        }
    }
}
