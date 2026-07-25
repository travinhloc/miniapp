package com.vault.vanishx.domain.model

data class MailboxRoom(
    val id: String,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val title: String? = null,
    val status: String = STATUS_ACTIVE,
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_EXPIRED = "expired"
    }
}
