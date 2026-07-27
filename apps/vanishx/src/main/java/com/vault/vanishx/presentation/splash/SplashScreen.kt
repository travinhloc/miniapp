package com.vault.vanishx.presentation.splash

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 1_800L
private const val SPLASH_DELAY_REDUCED_MS = 400L
private const val RIPPLE_DURATION_MS = 2_200
private const val RIPPLE_CENTER_Y_FRACTION = 0.42f
private const val RIPPLE_MAX_RADIUS_FRACTION = 0.55f
private const val RIPPLE_PHASE_STEP = 0.28f
private const val RIPPLE_ALPHA_PRIMARY = 0.22f
private const val RIPPLE_ALPHA_ACCENT = 0.18f
private const val RIPPLE_ALPHA_FAINT = 0.10f
private val LogoSize = 88.dp
private val BrandGap = 24.dp
private val TaglineGap = 8.dp
private val ProtectingGap = 32.dp

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    LaunchedEffect(Unit) {
        delay(if (reduceMotion) SPLASH_DELAY_REDUCED_MS else SPLASH_DELAY_MS)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg),
        contentAlignment = Alignment.Center,
    ) {
        if (!reduceMotion) {
            RippleBackdrop()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(LogoSize)
                    .background(VanishXColors.Surface, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "V",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VanishXColors.Primary,
                )
            }
            Spacer(modifier = Modifier.height(BrandGap))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(TaglineGap))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(ProtectingGap))
            Text(
                text = stringResource(R.string.splash_protecting),
                style = MaterialTheme.typography.labelMedium,
                color = VanishXColors.Muted,
            )
        }
    }
}

@Composable
private fun RippleBackdrop() {
    val transition = rememberInfiniteTransition(label = "splash_ripple")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RIPPLE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * RIPPLE_CENTER_Y_FRACTION)
        val maxR = size.minDimension * RIPPLE_MAX_RADIUS_FRACTION
        listOf(
            VanishXColors.Primary.copy(alpha = RIPPLE_ALPHA_PRIMARY),
            VanishXColors.Accent.copy(alpha = RIPPLE_ALPHA_ACCENT),
            VanishXColors.Primary.copy(alpha = RIPPLE_ALPHA_FAINT),
        ).forEachIndexed { index, color ->
            val t = ((pulse + index * RIPPLE_PHASE_STEP) % 1f)
            drawCircle(
                color = color,
                radius = maxR * t,
                center = center,
            )
        }
        drawCircle(color = Color.Transparent)
    }
}
