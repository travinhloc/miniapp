package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object RoomUiDimens {
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val composerGap = 8.dp
    val bubbleMaxWidth = 280.dp
    val bubbleCorner = 16.dp
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
}

internal const val ROOM_ID_DISPLAY_SUFFIX = 6
internal const val COMPOSER_MAX_LINES = 4
internal const val REPORT_REASON_MAX_LINES = 3
internal const val TTL_TICK_MS = 1_000L
internal const val ROOM_TTL_DEFAULT_MS = 24L * 60L * 60L * 1_000L
