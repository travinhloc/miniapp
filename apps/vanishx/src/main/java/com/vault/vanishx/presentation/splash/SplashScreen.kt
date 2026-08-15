package com.vault.vanishx.presentation.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

private const val EASE_BRAND_X1 = 0.2f
private const val EASE_BRAND_Y1 = 0.8f
private const val EASE_BRAND_X2 = 0.2f
private const val EASE_RIPPLE_X1 = 0.22f
private const val EASE_RIPPLE_Y1 = 0.61f
private const val EASE_RIPPLE_X2 = 0.36f
private val BrandEnterEasing = CubicBezierEasing(EASE_BRAND_X1, EASE_BRAND_Y1, EASE_BRAND_X2, 1f)
private val RippleEasing = CubicBezierEasing(EASE_RIPPLE_X1, EASE_RIPPLE_Y1, EASE_RIPPLE_X2, 1f)
private const val LogoCornerDp = 20
private const val BG_ACCENT_GLOW_ALPHA = 0.12f
private const val BG_PRIMARY_GLOW_ALPHA = 0.08f
private const val BG_CORE_PRIMARY_ALPHA = 0.06f
private const val BG_CORE_ACCENT_ALPHA = 0.04f
private const val CORE_FILL_PRIMARY_ALPHA = 0.28f
private const val CORE_FILL_ACCENT_ALPHA = 0.12f
private const val WAVE_GLOW_ALPHA = 0.35f
private val LogoMarkShape = RoundedCornerShape(LogoCornerDp.dp)

/** Covers brand-enter (~0.9s) without feeling stuck if identity is already warm. */
private const val SPLASH_MIN_MS = 1_200L
private const val SPLASH_MAX_MS = 2_400L
private const val SPLASH_REDUCED_MIN_MS = 0L
private const val SPLASH_REDUCED_MAX_MS = 400L

private const val BRAND_ENTER_MS = 900
private const val BRAND_ENTER_OFFSET_DP = 12f
private const val BRAND_ENTER_SCALE_FROM = 0.94f
private const val BRAND_ENTER_SCALE_DELTA = 0.06f

private const val GLOW_TOP_X = 0.5f
private const val GLOW_TOP_Y = -0.05f
private const val GLOW_TOP_RADIUS = 0.85f
private const val GLOW_BOTTOM_X = 0.1f
private const val GLOW_BOTTOM_Y = 0.85f
private const val GLOW_BOTTOM_RADIUS = 0.7f
private const val CORE_SPOT_X = 0.5f
private const val CORE_SPOT_Y = 0.42f
private const val CORE_SPOT_RADIUS = 0.45f

private const val LOGO_PULSE_MS = 2_400
private const val LOGO_GLOW_ALPHA_BASE = 0.22f
private const val LOGO_GLOW_ALPHA_DELTA = 0.13f
private const val LOGO_GLOW_RADIUS_BASE = 28f
private const val LOGO_GLOW_RADIUS_DELTA = 8f
private const val LOGO_GLOW_ACCENT_FACTOR = 0.45f

private const val CORE_BREATHE_MS = 2_400
private const val CORE_SCALE_BASE = 0.92f
private const val CORE_SCALE_DELTA = 0.16f
private const val CORE_ALPHA_BASE = 0.7f
private const val CORE_ALPHA_DELTA = 0.3f

private const val RIPPLE_DURATION_MS = 3_200
private const val RIPPLE_CENTER_Y = 0.42f
private const val RIPPLE_SCALE_FROM = 0.35f
private const val RIPPLE_SCALE_TO = 9.5f
private const val RIPPLE_FADE_IN_END = 0.12f
private const val RIPPLE_FADE_MID_END = 0.55f
private const val RIPPLE_OPACITY_PEAK = 0.85f
private const val RIPPLE_OPACITY_MID = 0.35f
private const val RIPPLE_FADE_MID_SPAN = 0.43f
private const val RIPPLE_FADE_OUT_SPAN = 0.45f
private const val RIPPLE_OPACITY_DROP = 0.50f
private const val RIPPLE_DELAY_0 = 0f
private const val RIPPLE_DELAY_1 = 0.125f
private const val RIPPLE_DELAY_2 = 0.25f
private const val RIPPLE_DELAY_3 = 0.375f
private const val RIPPLE_DELAY_4 = 0.5f
private const val RIPPLE_DELAY_5 = 0.625f
private const val RIPPLE_DELAY_6 = 0.75f
private val RippleDelayFractions = listOf(
    RIPPLE_DELAY_0,
    RIPPLE_DELAY_1,
    RIPPLE_DELAY_2,
    RIPPLE_DELAY_3,
    RIPPLE_DELAY_4,
    RIPPLE_DELAY_5,
    RIPPLE_DELAY_6,
)

private const val BOOT_ENTER_START = 0.35f
private const val BOOT_ENTER_SPAN = 0.65f
private const val WAVE_DURATION_MS = 1_400
private const val WAVE_IDLE_PROGRESS = 0.35f
private const val WAVE_BAR_WIDTH_FRACTION = 0.4f
private const val WAVE_START_OFFSET = 1.2f
private const val WAVE_TRAVEL = 3.2f

private val LogoMarkSize = 72.dp
private val LogoIconSize = 36.dp
private val BrandNameSize = 34.sp
private val BrandNameLineHeight = 40.sp
private val BrandNameTracking = 0.5.sp
private val TaglineMaxWidth = 220.dp
private val BrandToTaglineGap = 8.dp
private val LogoToBrandGap = 20.dp
private val ScreenPadding = 24.dp
private val BootBottomPadding = 48.dp
private val CoreGlowSize = 160.dp
private val CoreGlowOffset = 72.dp
private val RippleBaseRadius = 24.dp
private val RippleStrokePrimary = 1.5.dp
private val RippleStrokeAccent = 1.dp
private val WaveTrackWidth = 120.dp
private val WaveTrackHeight = 3.dp
private val WaveCorner = 2.dp
private val BootCaptionSize = 12.sp
private val BootCaptionTracking = 1.5.sp
private val BootRowGap = 12.dp

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    LaunchedEffect(reduceMotion, viewModel) {
        awaitSplashGate(
            bootstrapReady = viewModel.bootstrapReady,
            minDisplayMs = if (reduceMotion) SPLASH_REDUCED_MIN_MS else SPLASH_MIN_MS,
            maxDisplayMs = if (reduceMotion) SPLASH_REDUCED_MAX_MS else SPLASH_MAX_MS,
        )
        onFinished()
    }

    val brandEnter = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            brandEnter.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = BRAND_ENTER_MS, easing = BrandEnterEasing),
            )
        }
    }
    val enter = brandEnter.value
    val brandOffsetPx = with(density) { BRAND_ENTER_OFFSET_DP.dp.toPx() } * (1f - enter)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .drawBehind {
                val topGlow = Brush.radialGradient(
                    colors = listOf(
                        VanishXColors.Accent.copy(alpha = BG_ACCENT_GLOW_ALPHA),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * GLOW_TOP_X, size.height * GLOW_TOP_Y),
                    radius = size.minDimension * GLOW_TOP_RADIUS,
                )
                val bottomGlow = Brush.radialGradient(
                    colors = listOf(
                        VanishXColors.Primary.copy(alpha = BG_PRIMARY_GLOW_ALPHA),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * GLOW_BOTTOM_X, size.height * GLOW_BOTTOM_Y),
                    radius = size.minDimension * GLOW_BOTTOM_RADIUS,
                )
                val coreSpot = Brush.radialGradient(
                    colors = listOf(
                        VanishXColors.Primary.copy(alpha = BG_CORE_PRIMARY_ALPHA),
                        VanishXColors.Accent.copy(alpha = BG_CORE_ACCENT_ALPHA),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * CORE_SPOT_X, size.height * CORE_SPOT_Y),
                    radius = size.minDimension * CORE_SPOT_RADIUS,
                )
                drawRect(brush = topGlow)
                drawRect(brush = bottomGlow)
                drawRect(brush = coreSpot)
            },
    ) {
        if (!reduceMotion) {
            CoreGlow(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = CoreGlowOffset),
            )
            RippleField(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = ScreenPadding)
                .graphicsLayer {
                    alpha = enter
                    val scale = BRAND_ENTER_SCALE_FROM + BRAND_ENTER_SCALE_DELTA * enter
                    scaleX = scale
                    scaleY = scale
                    translationY = brandOffsetPx
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoMark(reduceMotion = reduceMotion)
            Spacer(modifier = Modifier.height(LogoToBrandGap))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = BrandNameSize,
                    lineHeight = BrandNameLineHeight,
                    letterSpacing = BrandNameTracking,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(BrandToTaglineGap))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(TaglineMaxWidth),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.splash_trust),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
                color = VanishXColors.Muted.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }

        BootRow(
            reduceMotion = reduceMotion,
            enterProgress = enter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = BootBottomPadding),
        )
    }
}

@Composable
private fun LogoMark(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "logo_pulse")
    val pulseRaw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LOGO_PULSE_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mark_pulse",
    )
    val pulse = if (reduceMotion) 0f else pulseRaw
    val glowAlpha = LOGO_GLOW_ALPHA_BASE + LOGO_GLOW_ALPHA_DELTA * pulse
    val glowRadius = LOGO_GLOW_RADIUS_BASE + LOGO_GLOW_RADIUS_DELTA * pulse

    Box(
        modifier = modifier
            .size(LogoMarkSize)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VanishXColors.Primary.copy(alpha = glowAlpha),
                            VanishXColors.Accent.copy(alpha = glowAlpha * LOGO_GLOW_ACCENT_FACTOR),
                            Color.Transparent,
                        ),
                    ),
                    radius = glowRadius.dp.toPx(),
                    center = center,
                )
            }
            .background(VanishXColors.Surface, LogoMarkShape)
            .border(1.dp, VanishXColors.Outline, LogoMarkShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = VanishXColors.Primary,
            modifier = Modifier.size(LogoIconSize),
        )
    }
}

@Composable
private fun CoreGlow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "core_glow")
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CORE_BREATHE_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val scale = CORE_SCALE_BASE + CORE_SCALE_DELTA * breathe
    val alpha = CORE_ALPHA_BASE + CORE_ALPHA_DELTA * breathe

    Canvas(
        modifier = modifier
            .size(CoreGlowSize)
            .scale(scale)
            .alpha(alpha),
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VanishXColors.Primary.copy(alpha = CORE_FILL_PRIMARY_ALPHA),
                    VanishXColors.Accent.copy(alpha = CORE_FILL_ACCENT_ALPHA),
                    Color.Transparent,
                ),
            ),
        )
    }
}

@Composable
private fun RippleField(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash_ripple")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RIPPLE_DURATION_MS, easing = RippleEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple_phase",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * RIPPLE_CENTER_Y)
        val baseRadius = RippleBaseRadius.toPx()
        RippleDelayFractions.forEachIndexed { index, delayFraction ->
            val t = ((pulse + (1f - delayFraction)) % 1f)
            val opacity = rippleOpacity(t)
            val scale = RIPPLE_SCALE_FROM + t * (RIPPLE_SCALE_TO - RIPPLE_SCALE_FROM)
            val isAccent = index % 2 == 1
            val color = (if (isAccent) VanishXColors.Accent else VanishXColors.Primary)
                .copy(alpha = opacity)
            val strokeWidth = if (isAccent) {
                RippleStrokeAccent.toPx()
            } else {
                RippleStrokePrimary.toPx()
            }
            drawCircle(
                color = color,
                radius = baseRadius * scale,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private fun rippleOpacity(t: Float): Float = when {
    t < RIPPLE_FADE_IN_END -> (t / RIPPLE_FADE_IN_END) * RIPPLE_OPACITY_PEAK
    t < RIPPLE_FADE_MID_END -> {
        RIPPLE_OPACITY_PEAK -
            ((t - RIPPLE_FADE_IN_END) / RIPPLE_FADE_MID_SPAN) * RIPPLE_OPACITY_DROP
    }
    else -> {
        (
            RIPPLE_OPACITY_MID *
                (1f - (t - RIPPLE_FADE_MID_END) / RIPPLE_FADE_OUT_SPAN)
            ).coerceAtLeast(0f)
    }
}

@Composable
private fun BootRow(
    reduceMotion: Boolean,
    enterProgress: Float,
    modifier: Modifier = Modifier,
) {
    val bootAlpha = if (reduceMotion) {
        1f
    } else {
        ((enterProgress - BOOT_ENTER_START) / BOOT_ENTER_SPAN).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(bootAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BootRowGap),
    ) {
        WaveLoader(reduceMotion = reduceMotion)
        Text(
            text = stringResource(R.string.splash_protecting),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = BootCaptionSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = BootCaptionTracking,
            ),
            color = VanishXColors.Muted,
        )
    }
}

@Composable
private fun WaveLoader(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wave_loader")
    val sweepRaw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val sweep = if (reduceMotion) WAVE_IDLE_PROGRESS else sweepRaw

    Canvas(
        modifier = modifier
            .width(WaveTrackWidth)
            .height(WaveTrackHeight),
    ) {
        val barWidth = size.width * WAVE_BAR_WIDTH_FRACTION
        val startX = -WAVE_START_OFFSET * size.width +
            sweep * (WAVE_TRAVEL * size.width + barWidth)
        val corner = CornerRadius(WaveCorner.toPx())
        drawRoundRect(
            color = VanishXColors.Outline,
            cornerRadius = corner,
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(VanishXColors.Accent, VanishXColors.Primary),
                startX = startX,
                endX = startX + barWidth,
            ),
            topLeft = Offset(startX, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = VanishXColors.Primary.copy(alpha = WAVE_GLOW_ALPHA),
            topLeft = Offset(startX, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
