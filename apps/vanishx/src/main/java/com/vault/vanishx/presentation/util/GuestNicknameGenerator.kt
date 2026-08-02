package com.vault.vanishx.presentation.util

import kotlin.math.abs

/**
 * Auto-fills the "Message Request" nickname field (story 7.6) so guests can accept without
 * typing first — deterministic per invite so it doesn't change across re-compositions.
 */
object GuestNicknameGenerator {
    private const val PREFIX = "User_"
    private const val SUFFIX_LENGTH = 4
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun generate(seed: String): String {
        var hash = 0
        for (char in seed) hash = hash * HASH_MULTIPLIER + char.code
        var value = abs(hash)
        val suffix = buildString {
            repeat(SUFFIX_LENGTH) {
                append(ALPHABET[value % ALPHABET.length])
                value /= ALPHABET.length
            }
        }
        return PREFIX + suffix
    }

    private const val HASH_MULTIPLIER = 31
}
