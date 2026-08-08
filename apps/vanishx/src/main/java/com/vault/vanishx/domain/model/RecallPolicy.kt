package com.vault.vanishx.domain.model

object RecallPolicy {
    /** Free + Pro may recall within this window; after that Pro only (E9-8). */
    const val FREE_WINDOW_MS: Long = 24L * 60L * 60L * 1_000L

    fun canRecallOutbound(
        sentAt: Long,
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val ageMs = (nowMs - sentAt).coerceAtLeast(0L)
        return ageMs < FREE_WINDOW_MS || isPro
    }
}
