package com.vault.vanishx.domain.repository

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom

interface MailboxRepository {
    suspend fun getActiveRooms(): List<MailboxRoom>
    suspend fun getAllRooms(): List<MailboxRoom>
    suspend fun getRoom(roomId: String): MailboxRoom?
    suspend fun upsertRoom(room: MailboxRoom)

    suspend fun getMessages(roomId: String): List<ChatMessage>
    suspend fun getMessage(messageId: String): ChatMessage?
    suspend fun upsertMessage(message: ChatMessage)
    suspend fun deleteExpiredMessages(nowMs: Long = System.currentTimeMillis()): Int
    suspend fun deleteMessagesForRoom(roomId: String): Int
}
