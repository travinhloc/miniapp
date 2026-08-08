package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RenameRoomUseCaseTest {

    @Test
    fun `renames local title`() = runTest {
        val repo: MailboxRepository = mockk(relaxed = true)
        val room = MailboxRoom(
            id = "r1",
            roomKey = "k",
            createdAt = 1L,
            expiresAt = 2L,
            role = MailboxRoom.ROLE_CREATOR,
        )
        coEvery { repo.getRoom("r1") } returns room

        val updated = RenameRoomUseCase(repo).invoke("r1", "  Night vault  ")
        updated.title shouldBe "Night vault"
        coVerify { repo.upsertRoom(match { it.title == "Night vault" }) }
    }

    @Test
    fun `rejects blank title`() = runTest {
        val repo: MailboxRepository = mockk(relaxed = true)
        coEvery { repo.getRoom("r1") } returns MailboxRoom(
            id = "r1",
            roomKey = "k",
            createdAt = 1L,
            expiresAt = 2L,
            role = MailboxRoom.ROLE_CREATOR,
        )
        shouldThrow<IllegalArgumentException> {
            RenameRoomUseCase(repo).invoke("r1", "   ")
        }
    }
}
