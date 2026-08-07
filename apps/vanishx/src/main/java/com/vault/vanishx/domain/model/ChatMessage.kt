package com.vault.vanishx.domain.model

data class ChatMessage(
    val id: String,
    val roomId: String,
    val body: String,
    val sentAt: Long,
    val expiresAt: Long,
    val direction: String,
    val recalled: Boolean = false,
    /** Opt-in Confide-style blur; peer learns flag from encrypted envelope. */
    val sensitive: Boolean = false,
) {
    companion object {
        const val DIRECTION_OUT = "out"
        const val DIRECTION_IN = "in"
    }
}
