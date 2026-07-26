package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomCryptoException
import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import timber.log.Timber
import javax.inject.Inject

data class SyncMailboxResult(
    val messages: List<ChatMessage>,
    val ingested: Int,
    val removedRemote: Int,
    val decryptFailures: Int,
)

class SyncRoomMailboxUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val cipher: RoomMessageCipher,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
) {
    suspend operator fun invoke(roomId: String): SyncMailboxResult {
        val room = mailboxRepository.getRoom(roomId)
            ?: error("Room not found")
        val now = System.currentTimeMillis()
        val resolved = room.copy(status = room.resolvedStatus(now))
        if (resolved.status != room.status) {
            mailboxRepository.upsertRoom(resolved)
        }

        if (resolved.status == MailboxRoom.STATUS_EXPIRED) {
            purgeExpiredRoom(roomId)
            return SyncMailboxResult(
                messages = emptyList(),
                ingested = 0,
                removedRemote = 0,
                decryptFailures = 0,
            )
        }

        mailboxRepository.deleteExpiredMessages(now)

        val myPub = identityRepository.ensureIdentity().publicKeyBase64
        val remoteMessages = remote.listMessages(roomId)
        var ingested = 0
        var removed = 0
        var decryptFailures = 0

        for (remoteMessage in remoteMessages) {
            when (
                processRemote(
                    room = resolved,
                    remoteMessage = remoteMessage,
                    myPub = myPub,
                    now = now,
                )
            ) {
                ProcessResult.INGESTED -> {
                    ingested++
                    removed++
                }
                ProcessResult.REMOVED_ONLY -> removed++
                ProcessResult.DECRYPT_FAIL -> decryptFailures++
                ProcessResult.SKIPPED -> Unit
            }
        }

        return SyncMailboxResult(
            messages = mailboxRepository.getMessages(roomId),
            ingested = ingested,
            removedRemote = removed,
            decryptFailures = decryptFailures,
        )
    }

    suspend fun ingestRemoteList(
        roomId: String,
        remoteMessages: List<RemoteMailboxMessage>,
    ): SyncMailboxResult {
        val room = mailboxRepository.getRoom(roomId) ?: error("Room not found")
        val now = System.currentTimeMillis()
        val resolved = room.copy(status = room.resolvedStatus(now))
        if (resolved.status != room.status) {
            mailboxRepository.upsertRoom(resolved)
        }

        if (resolved.status == MailboxRoom.STATUS_EXPIRED) {
            purgeExpiredRoom(roomId)
            return SyncMailboxResult(
                messages = emptyList(),
                ingested = 0,
                removedRemote = 0,
                decryptFailures = 0,
            )
        }

        mailboxRepository.deleteExpiredMessages(now)

        val myPub = identityRepository.ensureIdentity().publicKeyBase64
        var ingested = 0
        var removed = 0
        var decryptFailures = 0
        for (remoteMessage in remoteMessages) {
            when (
                processRemote(
                    room = resolved,
                    remoteMessage = remoteMessage,
                    myPub = myPub,
                    now = now,
                )
            ) {
                ProcessResult.INGESTED -> {
                    ingested++
                    removed++
                }
                ProcessResult.REMOVED_ONLY -> removed++
                ProcessResult.DECRYPT_FAIL -> decryptFailures++
                ProcessResult.SKIPPED -> Unit
            }
        }
        return SyncMailboxResult(
            messages = mailboxRepository.getMessages(roomId),
            ingested = ingested,
            removedRemote = removed,
            decryptFailures = decryptFailures,
        )
    }

    private suspend fun processRemote(
        room: MailboxRoom,
        remoteMessage: RemoteMailboxMessage,
        myPub: String,
        now: Long,
    ): ProcessResult {
        val early = processExpiredOrDuplicate(room.id, remoteMessage, now)
        if (early != null) return early

        val plaintext = decryptRemote(room, remoteMessage)
        return if (plaintext == null) {
            ProcessResult.DECRYPT_FAIL
        } else {
            ingestRemote(room, remoteMessage, myPub, plaintext)
            ProcessResult.INGESTED
        }
    }

    private suspend fun processExpiredOrDuplicate(
        roomId: String,
        remoteMessage: RemoteMailboxMessage,
        now: Long,
    ): ProcessResult? {
        val expired = remoteMessage.expiresAt > 0L && remoteMessage.expiresAt <= now
        val existing = mailboxRepository.getMessage(remoteMessage.messageId) != null
        if (!expired && !existing) return null
        runCatching { remote.deleteMessage(roomId, remoteMessage.messageId) }
        return ProcessResult.REMOVED_ONLY
    }

    private fun decryptRemote(
        room: MailboxRoom,
        remoteMessage: RemoteMailboxMessage,
    ): String? = try {
        cipher.decrypt(room.id, room.roomKey, remoteMessage.ciphertext)
    } catch (e: RoomCryptoException) {
        Timber.w(e, "Decrypt failed for %s", remoteMessage.messageId)
        null
    } catch (e: IllegalArgumentException) {
        Timber.w(e, "Decrypt rejected for %s", remoteMessage.messageId)
        null
    }

    private suspend fun ingestRemote(
        room: MailboxRoom,
        remoteMessage: RemoteMailboxMessage,
        myPub: String,
        plaintext: String,
    ) {
        val direction = if (remoteMessage.senderPub == myPub) {
            ChatMessage.DIRECTION_OUT
        } else {
            ChatMessage.DIRECTION_IN
        }
        mailboxRepository.upsertMessage(
            ChatMessage(
                id = remoteMessage.messageId,
                roomId = room.id,
                body = plaintext,
                sentAt = remoteMessage.createdAt,
                expiresAt = remoteMessage.expiresAt,
                direction = direction,
            ),
        )
        runCatching { remote.deleteMessage(room.id, remoteMessage.messageId) }
    }

    private enum class ProcessResult {
        INGESTED,
        REMOVED_ONLY,
        DECRYPT_FAIL,
        SKIPPED,
    }
}
