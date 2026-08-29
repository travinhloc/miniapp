package com.vault.vanishx.presentation.history

import com.vault.vanishx.domain.model.ConversationPreview
import com.vault.vanishx.domain.model.ConversationPreviewKind
import com.vault.vanishx.presentation.conversation.ConversationRowModel
import io.kotest.matchers.shouldBe
import org.junit.Test

class HistoryRoomListTest {

    @Test
    fun `History Open filter excludes waiting rooms`() {
        val waiting = item("wait", waiting = true, activityAt = 5L)
        val live = item("live", activityAt = 5L)
        waiting.matchesHistoryFilter(HistoryRoomFilter.Open) shouldBe false
        live.matchesHistoryFilter(HistoryRoomFilter.Open) shouldBe true
    }

    @Test
    fun `History Waiting filter lists only inactive rooms`() {
        val waiting = item("wait", waiting = true, activityAt = 5L)
        val live = item("live", activityAt = 5L)
        val expired = item("exp", waiting = true, expired = true, activityAt = 5L)
        waiting.matchesHistoryFilter(HistoryRoomFilter.Waiting) shouldBe true
        live.matchesHistoryFilter(HistoryRoomFilter.Waiting) shouldBe false
        expired.matchesHistoryFilter(HistoryRoomFilter.Waiting) shouldBe false
    }

    @Test
    fun `History sort is favorite then newest among open rooms`() {
        val waitingOld = item("wait", waiting = true, activityAt = 10L)
        val liveNew = item("live", activityAt = 30L)
        val favorite = item("fav", favorite = true, activityAt = 5L)
        val expired = item("exp", expired = true, activityAt = 40L)
        val left = item("left", left = true, activityAt = 50L)

        listOf(waitingOld, liveNew, favorite, expired, left)
            .sortedForHistory()
            .map { it.row.id } shouldBe listOf("fav", "live", "wait", "exp", "left")
    }

    private fun item(
        id: String,
        favorite: Boolean = false,
        waiting: Boolean = false,
        expired: Boolean = false,
        left: Boolean = false,
        activityAt: Long = 0L,
    ) = HistoryRoomItem(
        row = ConversationRowModel(
            id = id,
            displayName = id,
            initials = "A",
            avatarLocalPath = null,
            preview = ConversationPreview(
                kind = if (waiting) ConversationPreviewKind.Waiting else ConversationPreviewKind.Text,
                snippet = id,
                lastActivityAt = activityAt,
            ),
            unreadCount = 0,
            isFavorite = favorite,
            isMuted = false,
            isWaiting = waiting,
            isExpired = expired,
            isLeft = left,
            hasRoomClock = false,
            ttlFraction = 1f,
            remainingMs = 0L,
            activityAt = activityAt,
        ),
        meta = when {
            left -> HistoryRoomMeta.Left
            expired -> HistoryRoomMeta.Archived
            waiting -> HistoryRoomMeta.Waiting
            else -> HistoryRoomMeta.Member
        },
    )
}
