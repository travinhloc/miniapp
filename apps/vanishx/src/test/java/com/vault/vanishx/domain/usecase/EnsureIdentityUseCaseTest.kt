package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.repository.IdentityRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EnsureIdentityUseCaseTest {

    @Test
    fun `returns identity from repository`() = runTest {
        val identity = Identity(
            anonymousId = "vx_test",
            publicKeyBase64 = "pub",
        )
        val repository = mockk<IdentityRepository>()
        coEvery { repository.ensureIdentity() } returns identity

        val result = EnsureIdentityUseCase(repository)()

        result shouldBe identity
        coVerify(exactly = 1) { repository.ensureIdentity() }
    }
}
