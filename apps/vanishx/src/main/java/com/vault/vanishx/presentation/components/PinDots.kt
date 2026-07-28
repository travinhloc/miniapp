package com.vault.vanishx.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlin.math.roundToInt

private val DotSize = 12.dp
private val DotGap = 14.dp
private val DotBorder = 1.5.dp
private val DotShadow = 4.dp
private const val DEFAULT_PIN_LENGTH = 4
private const val SHAKE_PX = 8f
private const val SHAKE_MID_PX = 6f
private const val SHAKE_MS = 50
private const val GLOW_ALPHA = 0.45f

@Composable
fun PinDots(
    filled: Int,
    modifier: Modifier = Modifier,
    total: Int = DEFAULT_PIN_LENGTH,
    isError: Boolean = false,
    shakeToken: Int = 0,
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken == 0) return@LaunchedEffect
        offsetX.snapTo(0f)
        listOf(-SHAKE_PX, SHAKE_PX, -SHAKE_MID_PX, SHAKE_MID_PX, 0f).forEach { target ->
            offsetX.animateTo(target, animationSpec = tween(durationMillis = SHAKE_MS))
        }
    }
    val fillColor = if (isError) VanishXColors.Error else VanishXColors.Primary
    val emptyBorder = if (isError) VanishXColors.Error else VanishXColors.Outline
    Row(
        modifier = modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        repeat(total) { index ->
            val filledDot = index < filled
            Surface(
                modifier = Modifier
                    .size(DotSize)
                    .then(
                        if (filledDot) {
                            Modifier.shadow(
                                DotShadow,
                                CircleShape,
                                ambientColor = fillColor.copy(alpha = GLOW_ALPHA),
                            )
                        } else {
                            Modifier.border(DotBorder, emptyBorder, CircleShape)
                        },
                    ),
                shape = CircleShape,
                color = if (filledDot) fillColor else Color.Transparent,
            ) {}
        }
    }
}
