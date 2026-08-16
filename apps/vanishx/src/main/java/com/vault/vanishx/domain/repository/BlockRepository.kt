package com.vault.vanishx.domain.repository

import com.vault.vanishx.domain.model.BlockedPeer

interface BlockRepository {
    suspend fun isBlocked(peerPub: String): Boolean
    suspend fun listBlocked(): List<BlockedPeer>
    suspend fun block(peerPub: String, blockedAt: Long = System.currentTimeMillis())
    suspend fun unblock(peerPub: String)
}
