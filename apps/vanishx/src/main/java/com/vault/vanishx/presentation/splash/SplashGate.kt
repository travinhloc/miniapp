package com.vault.vanishx.presentation.splash

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Leaves splash when bootstrap is ready after [minDisplayMs], or when [maxDisplayMs] elapses.
 */
internal suspend fun awaitSplashGate(
    bootstrapReady: Flow<Boolean>,
    minDisplayMs: Long,
    maxDisplayMs: Long,
) {
    require(maxDisplayMs >= minDisplayMs) {
        "maxDisplayMs ($maxDisplayMs) must be >= minDisplayMs ($minDisplayMs)"
    }
    coroutineScope {
        val maxJob = launch { delay(maxDisplayMs) }
        val readyAfterMinJob = launch {
            delay(minDisplayMs)
            bootstrapReady.first { it }
        }
        select {
            maxJob.onJoin { }
            readyAfterMinJob.onJoin { }
        }
        maxJob.cancel()
        readyAfterMinJob.cancel()
    }
}
