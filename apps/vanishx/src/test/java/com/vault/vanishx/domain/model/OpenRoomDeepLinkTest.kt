package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class OpenRoomDeepLinkTest {

    @Test
    fun `parses vanishx open room uri`() {
        OpenRoomDeepLink.roomIdFrom("vanishx://open/abc123") shouldBe "abc123"
    }

    @Test
    fun `strips query and fragment`() {
        OpenRoomDeepLink.roomIdFrom("vanishx://open/room1?x=1#y") shouldBe "room1"
    }

    @Test
    fun `ignores invite and empty`() {
        OpenRoomDeepLink.roomIdFrom("vanishx://r/abc?k=1") shouldBe null
        OpenRoomDeepLink.roomIdFrom("vanishx://open/") shouldBe null
        OpenRoomDeepLink.roomIdFrom(null) shouldBe null
    }
}
