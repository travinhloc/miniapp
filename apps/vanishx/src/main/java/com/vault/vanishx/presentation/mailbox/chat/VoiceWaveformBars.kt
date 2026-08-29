@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zalo-like voice bars — stable shape per [seed], animated when [active].
 * [progress] 0…1 tints played portion during playback.
 */
@Composable
internal fun VoiceWaveformBars(
    seed: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    barCount: Int = 28,
    height: Dp = 28.dp,
    liveLevel: Float = 0f,
) {
    val heights = remember(seed, barCount) {
        val rnd = Random(seed.hashCode().toLong())
        FloatArray(barCount) { 0.25f + rnd.nextFloat() * 0.75f }
    }
    val phase = if (active) {
        val t = rememberInfiniteTransition(label = "voiceWave")
        t.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "voicePhase",
        ).value
    } else {
        0f
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val gap = size.width / (barCount * 2.2f)
        val barW = gap.coerceAtLeast(2f)
        val total = barCount * barW + (barCount - 1) * gap
        var x = ((size.width - total) / 2f).coerceAtLeast(0f)
        val maxH = size.height
        for (i in 0 until barCount) {
            val base = heights[i]
            val animated = if (active) {
                val wobble = abs(sin(phase + i * 0.45f))
                val live = if (liveLevel > 0f) 0.35f + liveLevel * 0.65f else 1f
                (base * (0.45f + 0.55f * wobble) * live).coerceIn(0.15f, 1f)
            } else {
                base
            }
            val h = maxH * animated
            val played = progress > 0f && (i + 0.5f) / barCount <= progress
            drawRoundRect(
                color = if (played) activeColor else inactiveColor,
                topLeft = Offset(x, (maxH - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
            x += barW + gap
        }
    }
}
