package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomCryptoException
import com.vault.vanishx.data.crypto.RoomBlobCipher
import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.media.LocalMediaStore
import com.vault.vanishx.data.remote.MediaStorageRemoteDataSource
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.MessagePlaintextCodec
import com.vault.vanishx.domain.repository.BlockRepository
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

@Suppress("LargeClass", "LongParameterList", "ComplexMethod", "ReturnCount", "MagicNumber")
class SyncRoomMailboxUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val remote: MailboxRemoteDataSource,
    private val cipher: RoomMessageCipher,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
    private val blockRepository: BlockRepository,
    private val refreshRoomMeta: RefreshRoomMetaUseCase,
    private val mediaRemote: MediaStorageRemoteDataSource,
    private val blobCipher: RoomBlobCipher,
    private val localMediaStore: LocalMediaStore,
) {
    suspend operator fun invoke(roomId: String): SyncMailboxResult {
        refreshRoomMeta(roomId)
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
                ProcessResult.INGESTED_PENDING_PEER -> ingested++
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
                ProcessResult.INGESTED_PENDING_PEER -> ingested++
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
        val early = processExpiredOrDuplicate(room.id, remoteMessage, myPub, now)
            ?: processBlockedPeer(room.id, remoteMessage, myPub)
        if (early != null) return early

        val plaintext = decryptRemote(room, remoteMessage)
        return if (plaintext == null) {
            ProcessResult.DECRYPT_FAIL
        } else {
            val removedRemote = ingestRemote(room, remoteMessage, myPub, plaintext)
            if (removedRemote) ProcessResult.INGESTED else ProcessResult.INGESTED_PENDING_PEER
        }
    }

    private suspend fun processBlockedPeer(
        roomId: String,
        remoteMessage: RemoteMailboxMessage,
        myPub: String,
    ): ProcessResult? {
        val shouldSkip = remoteMessage.senderPub == myPub ||
            !blockRepository.isBlocked(remoteMessage.senderPub)
        if (shouldSkip) return null
        runCatching { remote.deleteMessage(roomId, remoteMessage.messageId) }
        return ProcessResult.REMOVED_ONLY
    }

    private suspend fun processExpiredOrDuplicate(
        roomId: String,
        remoteMessage: RemoteMailboxMessage,
        myPub: String,
        now: Long,
    ): ProcessResult? {
        val expired = remoteMessage.expiresAt > 0L && remoteMessage.expiresAt <= now
        if (expired) {
            runCatching { remote.deleteMessage(roomId, remoteMessage.messageId) }
            return ProcessResult.REMOVED_ONLY
        }
        val existing = mailboxRepository.getMessage(remoteMessage.messageId) != null
        if (!existing) return null
        // Own outbound must stay on RTDB until the peer downloads (or TTL / recall).
        if (remoteMessage.senderPub == myPub) return ProcessResult.SKIPPED
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

    /**
     * @return true if remote ciphertext was deleted (inbound / delivered to us).
     */
    private suspend fun ingestRemote(
        room: MailboxRoom,
        remoteMessage: RemoteMailboxMessage,
        myPub: String,
        plaintext: String,
    ): Boolean {
        val direction = if (remoteMessage.senderPub == myPub) {
            ChatMessage.DIRECTION_OUT
        } else {
            ChatMessage.DIRECTION_IN
        }
        if (shouldRememberPeer(room, direction, remoteMessage.senderPub)) {
            mailboxRepository.upsertRoom(room.copy(peerPub = remoteMessage.senderPub))
        }
        val decoded = MessagePlaintextCodec.decode(plaintext)
        val attachment = decoded.attachment
        val localPath = attachment?.let { meta ->
            runCatching {
                val encrypted = mediaRemote.download(
                    room.id,
                    remoteMessage.messageId,
                    meta.attId,
                    MediaLimits.maxBytesForKind(meta.kind) + DOWNLOAD_OVERHEAD_BYTES,
                )
                localMediaStore.write(
                    room.id,
                    remoteMessage.messageId,
                    meta.attId,
                    blobCipher.decrypt(room.id, meta.attId, room.roomKey, encrypted),
                )
            }.onFailure { Timber.w(it, "Media download failed for %s", remoteMessage.messageId) }.getOrNull()
        }
        mailboxRepository.upsertMessage(
            ChatMessage(
                id = remoteMessage.messageId,
                roomId = room.id,
                body = decoded.text,
                sentAt = remoteMessage.createdAt,
                expiresAt = remoteMessage.expiresAt,
                direction = direction,
                sensitive = decoded.sensitive,
                replyToId = decoded.replyToId,
                mediaKind = attachment?.kind,
                mediaMime = attachment?.mime,
                mediaBytes = attachment?.bytes,
                mediaAttId = attachment?.attId,
                mediaWidth = attachment?.width,
                mediaHeight = attachment?.height,
                mediaFileName = attachment?.fileName,
                mediaLocalPath = localPath,
                mediaTransferStatus = if (attachment == null || localPath != null) {
                    ChatMessage.MEDIA_READY
                } else {
                    ChatMessage.MEDIA_FAILED
                },
            ),
        )
        // RTDB is a peer pickup queue: only the recipient removes ciphertext.
        if (direction == ChatMessage.DIRECTION_IN) {
            runCatching { remote.deleteMessage(room.id, remoteMessage.messageId) }
            return true
        }
        return false
    }

    private fun shouldRememberPeer(
        room: MailboxRoom,
        direction: String,
        senderPub: String,
    ): Boolean =
        direction == ChatMessage.DIRECTION_IN &&
            room.peerPub.isNullOrBlank() &&
            senderPub.isNotBlank()

    private enum class ProcessResult {
        INGESTED,
        /** Ingested own outbound; left on RTDB for offline peer. */
        INGESTED_PENDING_PEER,
        REMOVED_ONLY,
        DECRYPT_FAIL,
        SKIPPED,
    }

    private companion object {
        const val DOWNLOAD_OVERHEAD_BYTES = 64 * 1024
    }
}
