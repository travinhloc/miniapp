package com.vault.vanishx.data.push

import com.vault.vanishx.domain.model.IncomingPushType
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class IncomingPushHandlerTest {

    private val mailboxRepository: MailboxRepository = mockk()
    private val identityRepository: IdentityRepository = mockk()
    private val blockRepository: BlockRepository = mockk()
    private val tracker = RoomForegroundTracker()
    private val notificationHelper: RoomNotificationHelper = mockk(relaxed = true)
    private val handler = IncomingPushHandler(
        mailboxRepository,
        identityRepository,
        blockRepository,
        tracker,
        notificationHelper,
    )

    @Test
    fun `shows notification when policy allows`() = runTest {
        coEvery { mailboxRepository.getRoom("room1") } returns MailboxRoom("room1", "k")
        coEvery { identityRepository.ensureIdentity() } returns
            com.vault.vanishx.domain.model.Identity("vx", "me")
        coEvery { blockRepository.isBlocked("peer") } returns false

        handler.handle(
            mapOf("roomId" to "room1", "type" to "message", "senderPub" to "peer"),
        )

        coVerify {
            notificationHelper.showRoomPushNotification("room1", IncomingPushType.MESSAGE)
        }
    }

    @Test
    fun `does not notify when muted`() = runTest {
        coEvery { mailboxRepository.getRoom("room1") } returns
            MailboxRoom("room1", "k", muted = true)
        coEvery { identityRepository.ensureIdentity() } returns
            com.vault.vanishx.domain.model.Identity("vx", "me")
        coEvery { blockRepository.isBlocked(any()) } returns false

        handler.handle(mapOf("roomId" to "room1", "senderPub" to "peer"))

        coVerify(exactly = 0) { notificationHelper.showRoomPushNotification(any(), any()) }
    }
}
