package com.vault.vanishx.presentation.splash

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
import android.provider.Settings
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
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
        delay(if (reduceMotion) 400L else 1_800L)
        onFinished()
    }

    Box(
        modifier = Modifier
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
                    .size(88.dp)
                    .background(VanishXColors.Surface, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "V",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VanishXColors.Primary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
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
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val maxR = size.minDimension * 0.55f
        listOf(
            VanishXColors.Primary.copy(alpha = 0.22f),
            VanishXColors.Accent.copy(alpha = 0.18f),
            VanishXColors.Primary.copy(alpha = 0.10f),
        ).forEachIndexed { index, color ->
            val t = ((pulse + index * 0.28f) % 1f)
            drawCircle(
                color = color,
                radius = maxR * t,
                center = center,
            )
        }
        drawCircle(color = Color.Transparent)
    }
}
