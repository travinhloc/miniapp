package com.vault.vanishx.data.remote

data class RemotePresence(
    val deviceId: String,
    val online: Boolean,
    val updatedAt: Long,
)

data class RemoteReadWatermark(
    val deviceId: String,
    val messageId: String,
    val updatedAt: Long,
)

data class RemoteTyping(
    val deviceId: String,
    val at: Long,
)

data class RemoteReaction(
    val messageId: String,
    val deviceId: String,
    val emoji: String,
    val at: Long,
)
