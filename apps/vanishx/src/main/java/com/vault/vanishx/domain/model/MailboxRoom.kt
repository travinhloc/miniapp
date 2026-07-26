package com.vault.vanishx.domain.model

data class MailboxRoom(
    val id: String,
    val roomKey: String,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val title: String? = null,
    val status: String = STATUS_ACTIVE,
    val role: String = ROLE_MEMBER,
    /** Peer Ed25519 public key (Base64), when known from meta or inbound messages. */
    val peerPub: String? = null,
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_LEFT = "left"
        const val ROLE_CREATOR = "creator"
        const val ROLE_MEMBER = "member"
    }

    fun resolvedStatus(nowMs: Long = System.currentTimeMillis()): String =
        if (expiresAt > 0L && nowMs >= expiresAt) STATUS_EXPIRED else status
}

enum class RoomTtlOption(val durationMs: Long) {
    ONE_HOUR(RoomTtlMs.ONE_HOUR),
    SIX_HOURS(RoomTtlMs.SIX_HOURS),
    ONE_DAY(RoomTtlMs.ONE_DAY),
    SEVEN_DAYS(RoomTtlMs.SEVEN_DAYS),
}

private object RoomTtlMs {
    const val ONE_HOUR = 3_600_000L
    const val SIX_HOURS = 21_600_000L
    const val ONE_DAY = 86_400_000L
    const val SEVEN_DAYS = 604_800_000L
}

data class RoomInvite(
    val roomId: String,
    val roomKey: String,
    val expiresAt: Long? = null,
) {
    fun toUriString(): String = InviteUriCodec.format(this)
}
