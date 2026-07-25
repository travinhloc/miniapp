package com.vault.vanishx.domain.repository

/**
 * Internal hook for Panic wipe (story 3.2). No UI in 1.3.
 */
interface LocalDatabaseWiper {
    suspend fun wipe()
}
