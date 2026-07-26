package com.vault.vanishx.domain.repository

interface BlockRepository {
    suspend fun isBlocked(peerPub: String): Boolean
    suspend fun block(peerPub: String, blockedAt: Long = System.currentTimeMillis())
}
