package com.vault.vanishx.domain.model

data class BlockedPeer(
    val peerPub: String,
    val blockedAt: Long,
)
