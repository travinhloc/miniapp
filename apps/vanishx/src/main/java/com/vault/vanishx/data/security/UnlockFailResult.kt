package com.vault.vanishx.data.security

/**
 * Result of recording a failed unlock attempt within the current streak.
 */
sealed interface UnlockFailResult {
    data class Wrong(val attemptsLeft: Int) : UnlockFailResult
    data class Cooldown(
        val untilEpochMs: Long,
        val durationMs: Long,
        val tierIndex: Int,
    ) : UnlockFailResult
    data object Wipe : UnlockFailResult
}
