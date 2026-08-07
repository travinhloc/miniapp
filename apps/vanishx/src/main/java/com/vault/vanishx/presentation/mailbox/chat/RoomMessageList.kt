package com.vault.vanishx.presentation.mailbox.chat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.util.formatRemainingMs
import kotlinx.coroutines.delay

@Composable
@Suppress("UnstableCollections")
internal fun RoomMessageList(
    messages: List<ChatMessage>,
    expiresAt: Long?,
    activatedAt: Long?,
    isExpired: Boolean,
    onOpenSafety: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val showTtl = !isExpired && expiresAt != null && expiresAt > 0L
    val view = LocalView.current
    var wasExpired by remember { mutableStateOf(isExpired) }
    val dissolve = remember { Animatable(1f) }

    LaunchedEffect(showTtl, expiresAt) {
        if (!showTtl) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(TTL_TICK_MS)
        }
    }

    LaunchedEffect(isExpired) {
        if (isExpired && !wasExpired) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            dissolve.snapTo(1f)
            dissolve.animateTo(0.35f, tween(700))
        }
        wasExpired = isExpired
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + if (showTtl) 1 else 0)
        }
    }

    val expiryProgress = roomExpiryProgress(expiresAt, activatedAt, nowMs)
    val showAura = !isExpired && shouldShowBubbleAura(expiryProgress)

    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = RoomUiDimens.spacingMedium),
            contentAlignment = Alignment.Center,
        ) {
            E2eEmptyCard(onClick = onOpenSafety)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = dissolve.value
                    scaleX = 0.96f + 0.04f * dissolve.value
                    scaleY = 0.96f + 0.04f * dissolve.value
                },
            contentPadding = PaddingValues(
                horizontal = RoomUiDimens.spacingMedium,
                vertical = RoomUiDimens.spacingSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showTtl) {
                val ttlAt = expiresAt!!
                item(key = "ttl-chip") {
                    TtlChip(remainingMs = (ttlAt - nowMs).coerceAtLeast(0L))
                }
            }
            items(messages, key = { it.id }) { message ->
                RoomMessageBubble(
                    message = message,
                    showAura = showAura,
                    auraIntensity = expiryProgress,
                )
            }
        }
    }
}

@Composable
internal fun E2eEmptyCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(RoomUiDimens.cardCorner),
        color = VanishXColors.Surface2,
        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = RoomUiDimens.e2eBorderAlpha)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = RoomUiDimens.e2eLockTint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.room_empty_e2e_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.room_empty_e2e_body),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                textAlign = TextAlign.Center,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.room_e2e_tap_hint),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = VanishXColors.Primary,
            )
        }
    }
}

@Composable
private fun TtlChip(remainingMs: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VanishXColors.Accent.copy(alpha = RoomUiDimens.ttlBgAlpha),
            border = BorderStroke(1.dp, VanishXColors.Accent.copy(alpha = RoomUiDimens.ttlBorderAlpha)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.room_ttl_chip, formatRemainingMs(remainingMs)),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    ),
                    color = VanishXColors.Accent,
                )
            }
        }
    }
}

@Composable
internal fun RoomActiveBody(
    messages: List<ChatMessage>,
    expiresAt: Long?,
    activatedAt: Long?,
    isExpired: Boolean,
    isSyncing: Boolean,
    onOpenSafety: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoomMessageList(
            messages = messages,
            expiresAt = expiresAt,
            activatedAt = activatedAt,
            isExpired = isExpired,
            onOpenSafety = onOpenSafety,
            modifier = Modifier.weight(1f),
        )

        if (isSyncing) {
            Text(
                text = stringResource(R.string.room_syncing),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
