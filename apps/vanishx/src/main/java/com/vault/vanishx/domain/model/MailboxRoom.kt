package com.vault.vanishx.domain.model

data class MailboxRoom(
    val id: String,
    val roomKey: String,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val title: String? = null,
    val status: String = STATUS_ACTIVE,
    val role: String = ROLE_MEMBER,
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_EXPIRED = "expired"
        const val ROLE_CREATOR = "creator"
        const val ROLE_MEMBER = "member"
    }

    fun resolvedStatus(nowMs: Long = System.currentTimeMillis()): String =
        if (expiresAt > 0L && nowMs >= expiresAt) STATUS_EXPIRED else status
}

enum class RoomTtlOption(val durationMs: Long) {
    ONE_HOUR(60L * 60L * 1000L),
    SIX_HOURS(6L * 60L * 60L * 1000L),
    ONE_DAY(24L * 60L * 60L * 1000L),
    SEVEN_DAYS(7L * 24L * 60L * 60L * 1000L),
}

data class RoomInvite(
    val roomId: String,
    val roomKey: String,
    val expiresAt: Long? = null,
) {
    fun toUriString(): String = InviteUriCodec.format(this)
}
