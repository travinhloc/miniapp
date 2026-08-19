package com.vault.vanishx.presentation.home

import com.vault.vanishx.domain.model.ConversationPreview
import com.vault.vanishx.domain.model.ConversationPreviewKind
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.conversation.ConversationRowModel
import io.kotest.matchers.shouldBe
import org.junit.Test

class HomeRoomListTest {

    @Test
    fun `waiting creator rooms are not listed on Home`() {
        waitingRoom().isHomeListEligible(nowMs = 1_000L) shouldBe false
    }

    @Test
    fun `favorite waiting rooms still leave Home`() {
        waitingRoom().copy(favorite = true).isHomeListEligible(nowMs = 1_000L) shouldBe false
    }

    @Test
    fun `left rooms are not listed on Home`() {
        liveRoom().copy(status = MailboxRoom.STATUS_LEFT).isHomeListEligible(nowMs = 1_000L) shouldBe false
    }

    @Test
    fun `activated rooms stay on Home`() {
        liveRoom().isHomeListEligible(nowMs = 1_000L) shouldBe true
    }

    @Test
    fun `Home sort is favorite then newest then oldest`() {
        val favoriteOld = row("fav-old", favorite = true, activityAt = 10L)
        val favoriteNew = row("fav-new", favorite = true, activityAt = 40L)
        val liveOld = row("live-old", activityAt = 20L)
        val liveNew = row("live-new", activityAt = 30L)

        listOf(liveOld, favoriteOld, liveNew, favoriteNew)
            .sortedForHome()
            .map { it.id } shouldBe listOf("fav-new", "fav-old", "live-new", "live-old")
    }

    @Test
    fun `toConversationRow uses createdAt when there is no last activity`() {
        val row = waitingRoom().copy(createdAt = 42L).toConversationRow(
            lastMessage = null,
            messages = emptyList(),
            isPro = false,
            nowMs = 1_000L,
        )
        row.activityAt shouldBe 42L
        row.preview.lastActivityAt shouldBe 42L
    }

    private fun waitingRoom() = MailboxRoom(
        id = "wait",
        roomKey = "k",
        createdAt = 100L,
        status = MailboxRoom.STATUS_ACTIVE,
        role = MailboxRoom.ROLE_CREATOR,
        activatedAt = 0L,
    )

    private fun liveRoom() = waitingRoom().copy(
        id = "live",
        activatedAt = 1L,
        title = "Live",
    )

    private fun row(
        id: String,
        favorite: Boolean = false,
        activityAt: Long = 0L,
    ) = ConversationRowModel(
        id = id,
        displayName = id,
        initials = "A",
        avatarLocalPath = null,
        preview = ConversationPreview(ConversationPreviewKind.Text, snippet = id, lastActivityAt = activityAt),
        unreadCount = 0,
        isFavorite = favorite,
        isMuted = false,
        isWaiting = false,
        isExpired = false,
        isLeft = false,
        hasRoomClock = false,
        ttlFraction = 1f,
        remainingMs = 0L,
        activityAt = activityAt,
    )
}
