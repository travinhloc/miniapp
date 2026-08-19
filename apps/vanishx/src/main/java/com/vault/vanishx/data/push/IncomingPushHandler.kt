package com.vault.vanishx.data.push

import com.vault.vanishx.domain.model.IncomingPushContext
import com.vault.vanishx.domain.model.IncomingPushParser
import com.vault.vanishx.domain.model.IncomingPushPolicy
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingPushHandler @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
    private val blockRepository: BlockRepository,
    private val foregroundTracker: RoomForegroundTracker,
    private val notificationHelper: RoomNotificationHelper,
) {
    suspend fun handle(data: Map<String, String>) {
        val push = IncomingPushParser.parse(data) ?: return
        val room = runCatching { mailboxRepository.getRoom(push.roomId) }.getOrNull()
        val myPub = runCatching { identityRepository.ensureIdentity().publicKeyBase64 }.getOrNull()
        val senderBlocked = push.senderPub
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { blockRepository.isBlocked(it) }.getOrDefault(false) }
            ?: false
        val show = IncomingPushPolicy.shouldNotify(
            IncomingPushContext(
                push = push,
                room = room,
                myPub = myPub,
                foregroundRoomId = foregroundTracker.roomId,
                senderBlocked = senderBlocked,
            ),
        )
        if (!show) {
            Timber.d("FCM dropped room=%s type=%s", push.roomId, push.type)
            return
        }
        notificationHelper.showRoomPushNotification(push.roomId, push.type)
    }
}
