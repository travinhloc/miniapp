package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object RoomUiDimens {
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val composerGap = 8.dp
    val bubbleMaxWidth = 280.dp
    val bubbleCorner = 16.dp
    val mediaCorner = 16.dp
    /** Reaction pill hangs this far past the bubble/media bottom edge. */
    val reactionHangOffset = 10.dp
    /** End inset for media timestamp so it does not sit under the reaction pill. */
    val reactionMetaEndClearance = 40.dp
    val bubbleTailCorner = 4.dp
    val cardCorner = 16.dp
    val topBarHeight = 56.dp
    val avatarSize = 36.dp
    val waitingHeroAvatar = 56.dp
    val waitingQrSize = 148.dp
    val composerPillRadius = 24.dp
    val composerFieldHeight = 48.dp
    val sendButtonSize = 48.dp
    val qrSize = 180.dp
    val handshakeBannerTextWidth = 260.dp
    val bentoCardCorner = 14.dp
    val radarBorder = 2.dp
    val bubbleInColor = Color(0xFF21262D)
    val e2eLockTint = Color(0xFF5B9FFF)

    const val dividerAlpha = 0.6f
    const val e2eBorderAlpha = 0.1f
    const val ttlBgAlpha = 0.12f
    const val ttlBorderAlpha = 0.25f
    const val timeAlpha = 0.55f
    const val checkAlpha = 0.85f
    const val recalledAlpha = 0.7f
    const val composerLockedAlpha = 0.55f
    const val composerLockedOverlayAlpha = 0.72f
    const val sendDisabledAlpha = 0.35f
    /** Room TTL fraction below which bubble aura starts (Free Host clock). */
    const val auraThresholdFraction = 0.25f
    const val auraMaxAlpha = 0.55f

    const val avatarPulseScale = 1.55f
    const val radarDurationMs = 1_800
    const val radarLagOffset = 0.45f
    const val radarScaleExtra = 0.85f
    const val radarAlphaFactor = 0.55f
    const val badgeGlowMin = 0.35f
    const val badgeGlowMax = 0.75f
    const val badgeGlowDurationMs = 1_100
    const val waitingTitleGlowMin = 0.45f
    const val waitingTitleGlowMax = 1f
    const val waitingTitleDurationMs = 1_200
    const val waitingTitleBaseAlpha = 0.65f
    const val waitingTitleGlowSpan = 0.35f
    const val waitingTitleHaloAlpha = 0.12f
    const val waitingTitleHaloScale = 0.9f
    const val waitingCardBorderAlpha = 0.45f
    const val nudgeSurfaceAlpha = 0.08f
    const val nudgeBorderAlpha = 0.22f
    const val liveGlowMin = 0.35f
    const val liveGlowMax = 0.85f
    const val liveGlowDurationMs = 1_400
    const val livePillBgBase = 0.12f
    const val livePillBgGlow = 0.1f
    const val livePillBorderBase = 0.4f
    const val livePillBorderGlow = 0.35f
    const val livePillHaloAlpha = 0.12f
    const val livePillHaloScale = 0.55f
    const val waitingBadgeBgBase = 0.12f
    const val waitingBadgeBgGlow = 0.22f
    const val waitingBadgeBorderBase = 0.45f
    const val waitingBadgeBorderGlow = 0.4f
    const val headerNeonDividerWaiting = 0.35f
    const val headerNeonDividerLive = 0.28f
    const val glassBorderAlpha = 0.12f
    const val dissolveMinAlpha = 0.35f
    const val dissolveDurationMs = 700
    const val dissolveScaleMin = 0.96f
    const val dissolveScaleSpan = 0.04f
    const val fingerprintHexLen = 32
    const val fingerprintGroupSize = 4
}

internal const val ROOM_ID_DISPLAY_SUFFIX = 6
internal const val COMPOSER_MAX_LINES = 4
internal const val REPORT_REASON_MAX_LINES = 3
internal const val TTL_TICK_MS = 1_000L
internal const val ROOM_TTL_DEFAULT_MS = 24L * 60L * 60L * 1_000L