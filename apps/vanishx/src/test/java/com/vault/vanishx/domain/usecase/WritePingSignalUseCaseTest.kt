package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomSignal
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.repository.IdentityRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WritePingSignalUseCaseTest {

    @Test
    fun `writes ping signal with local public key`() = runTest {
        val remote = InMemoryMailboxRemoteDataSource()
        val identityRepository: IdentityRepository = mockk()
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx", "myPub")
        val useCase = WritePingSignalUseCase(remote, identityRepository)

        useCase("room1")

        val signals = remote.signalsFor("room1")
        signals.size shouldBe 1
        signals.single().type shouldBe RemoteRoomSignal.TYPE_PING
        signals.single().fromPub shouldBe "myPub"
    }
}
