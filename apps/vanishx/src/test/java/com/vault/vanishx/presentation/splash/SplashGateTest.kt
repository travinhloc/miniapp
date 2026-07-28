package com.vault.vanishx.presentation.splash

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashGateTest {

    @Test
    fun `exits early after min when bootstrap already ready`() = runTest {
        val ready = MutableStateFlow(true)
        val job = launch {
            awaitSplashGate(ready, minDisplayMs = 800L, maxDisplayMs = 1_800L)
        }
        advanceTimeBy(799L)
        runCurrent()
        job.isCompleted shouldBe false
        advanceTimeBy(1L)
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 800L
    }

    @Test
    fun `waits for bootstrap then exits without hitting max`() = runTest {
        val ready = MutableStateFlow(false)
        val job = launch {
            awaitSplashGate(ready, minDisplayMs = 800L, maxDisplayMs = 1_800L)
        }
        advanceTimeBy(800L)
        runCurrent()
        job.isCompleted shouldBe false

        advanceTimeBy(200L)
        ready.value = true
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 1_000L
    }

    @Test
    fun `exits at max even if bootstrap never ready`() = runTest {
        val ready = MutableStateFlow(false)
        val job = launch {
            awaitSplashGate(ready, minDisplayMs = 800L, maxDisplayMs = 1_800L)
        }
        advanceTimeBy(1_799L)
        runCurrent()
        job.isCompleted shouldBe false
        advanceTimeBy(1L)
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 1_800L
    }
}
