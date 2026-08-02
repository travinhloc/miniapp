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
            awaitSplashGate(ready, minDisplayMs = 1_200L, maxDisplayMs = 2_400L)
        }
        advanceTimeBy(1_199L)
        runCurrent()
        job.isCompleted shouldBe false
        advanceTimeBy(1L)
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 1_200L
    }

    @Test
    fun `waits for bootstrap then exits without hitting max`() = runTest {
        val ready = MutableStateFlow(false)
        val job = launch {
            awaitSplashGate(ready, minDisplayMs = 1_200L, maxDisplayMs = 2_400L)
        }
        advanceTimeBy(1_200L)
        runCurrent()
        job.isCompleted shouldBe false

        advanceTimeBy(200L)
        ready.value = true
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 1_400L
    }

    @Test
    fun `exits at max even if bootstrap never ready`() = runTest {
        val ready = MutableStateFlow(false)
        val job = launch {
            awaitSplashGate(ready, minDisplayMs = 1_200L, maxDisplayMs = 2_400L)
        }
        advanceTimeBy(2_399L)
        runCurrent()
        job.isCompleted shouldBe false
        advanceTimeBy(1L)
        runCurrent()
        job.isCompleted shouldBe true
        testScheduler.currentTime shouldBe 2_400L
    }
}
