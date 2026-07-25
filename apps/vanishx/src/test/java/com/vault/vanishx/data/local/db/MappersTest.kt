package com.vault.vanishx.data.local.db

import com.vault.vanishx.domain.model.MailboxRoom
import io.kotest.matchers.shouldBe
import org.junit.Test

class MappersTest {

    @Test
    fun `maps room entity round trip`() {
        val domain = MailboxRoom(
            id = "r1",
            createdAt = 10L,
            expiresAt = 20L,
            title = "t",
            status = MailboxRoom.STATUS_ACTIVE,
        )

        domain.toEntity().toDomain() shouldBe domain
    }
}
