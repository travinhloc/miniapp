package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomAvatar(
    letter: String,
    pulse: Boolean = false,
    size: Dp = RoomUiDimens.avatarSize,
    neonColor: Color = VanishXColors.NeonAmber,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(if (pulse) size * RoomUiDimens.avatarPulseScale else size),
    ) {
        if (pulse) {
            WaitingRadarRings(color = neonColor, baseSize = size)
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(VanishXColors.Primary, VanishXColors.Accent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun WaitingRadarRings(
    color: Color,
    baseSize: Dp,
) {
    val transition = rememberInfiniteTransition(label = "waitingRadar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RoomUiDimens.radarDurationMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarProgress",
    )
    val lag = ((progress + RoomUiDimens.radarLagOffset) % 1f)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RadarRing(progress = progress, color = color, baseSize = baseSize)
        RadarRing(progress = lag, color = color, baseSize = baseSize)
    }
}

@Composable
private fun RadarRing(
    progress: Float,
    color: Color,
    baseSize: Dp,
) {
    val scale = 1f + progress * RoomUiDimens.radarScaleExtra
    val alpha = (1f - progress).coerceIn(0f, 1f) * RoomUiDimens.radarAlphaFactor
    Box(
        modifier = Modifier
            .size(baseSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .border(width = RoomUiDimens.radarBorder, color = color, shape = CircleShape),
    )
}

@Composable
internal fun WaitingBadgeGlow(
    content: @Composable (glow: Float) -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "waitingBadge")
    val glow by transition.animateFloat(
        initialValue = RoomUiDimens.badgeGlowMin,
        targetValue = RoomUiDimens.badgeGlowMax,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RoomUiDimens.badgeGlowDurationMs,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waitingGlow",
    )
    content(glow)
}
