package com.vault.vanishx.presentation.mailbox

import io.kotest.matchers.shouldBe
import org.junit.Test

class PendingOpenRoomStoreTest {

    @Test
    fun `offer then consume clears pending room`() {
        val store = PendingOpenRoomStore()
        store.offer(" room1 ")
        store.roomId.value shouldBe "room1"
        store.consume()
        store.roomId.value shouldBe null
    }

    @Test
    fun `offer same id again still updates`() {
        val store = PendingOpenRoomStore()
        store.offer("room1")
        store.offer("room1")
        store.roomId.value shouldBe "room1"
    }
}
