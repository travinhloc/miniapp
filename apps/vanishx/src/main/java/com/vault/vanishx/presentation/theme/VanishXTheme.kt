package com.vault.vanishx.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniapp.core.ui.theme.AppColors
import com.miniapp.core.ui.theme.AppShapes
import com.miniapp.core.ui.theme.AppTypography
import com.miniapp.core.ui.theme.ComposeTheme

private val NightColorScheme = darkColorScheme(
    primary = VanishXColors.Primary,
    onPrimary = VanishXColors.OnPrimary,
    primaryContainer = VanishXColors.Primary,
    onPrimaryContainer = VanishXColors.OnPrimary,
    secondary = VanishXColors.Accent,
    onSecondary = VanishXColors.OnSurface,
    secondaryContainer = VanishXColors.Surface2,
    onSecondaryContainer = VanishXColors.OnSurface,
    tertiary = VanishXColors.Accent,
    onTertiary = VanishXColors.OnSurface,
    background = VanishXColors.Bg,
    onBackground = VanishXColors.OnSurface,
    surface = VanishXColors.Surface,
    onSurface = VanishXColors.OnSurface,
    surfaceVariant = VanishXColors.Surface2,
    onSurfaceVariant = VanishXColors.Muted,
    outline = VanishXColors.Outline,
    error = VanishXColors.Error,
    onError = VanishXColors.OnSurface,
)

private val NightShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val NightTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

private class VanishXAppColors : AppColors() {
    // ComposeTheme mutates colorScheme to DarkColorPalette — always expose Night.
    override val themeColors get() = NightColorScheme
}

private object VanishXAppShapes : AppShapes {
    override val themeShapes: Shapes get() = NightShapes
}

private object VanishXAppTypography : AppTypography {
    override val themeTypography: Typography get() = NightTypography
}

val LocalVanishXExtra = staticCompositionLocalOf { VanishXExtraColors() }

data class VanishXExtraColors(
    val accent: androidx.compose.ui.graphics.Color = VanishXColors.Accent,
    val muted: androidx.compose.ui.graphics.Color = VanishXColors.Muted,
    val warn: androidx.compose.ui.graphics.Color = VanishXColors.Warn,
    val ok: androidx.compose.ui.graphics.Color = VanishXColors.Ok,
    val surface2: androidx.compose.ui.graphics.Color = VanishXColors.Surface2,
    val cardRadius: Dp = 16.dp,
    val buttonRadius: Dp = 8.dp,
)

object VanishXTheme {
    val extras: VanishXExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVanishXExtra.current
}

/** Dark-only M2 Night theme for VanishX (story 5.1). */
@Composable
fun VanishXTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalVanishXExtra provides VanishXExtraColors()) {
        ComposeTheme(
            colors = VanishXAppColors(),
            shapes = VanishXAppShapes,
            typography = VanishXAppTypography,
            darkTheme = true,
            content = content,
        )
    }
}
