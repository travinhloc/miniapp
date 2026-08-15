@file:Suppress("ComplexMethod", "UnstableCollections", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.util.formatRemainingMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
@Composable
@Suppress("UnstableCollections")
internal fun RoomMessageList(
    messages: List<ChatMessage>,
    expiresAt: Long?,
    activatedAt: Long?,
    isExpired: Boolean,
    onOpenSafety: () -> Unit,
    modifier: Modifier = Modifier,
    reactionsByMessage: Map<String, Map<String, Int>> = emptyMap(),
    peerReadWatermarkId: String? = null,
    onLongPressMessage: (ChatMessage) -> Unit = {},
    onMediaClick: (ChatMessage) -> Unit = {},
    scrollToMessageId: String? = null,
    highlightMessageId: String? = null,
    wallpaperPath: String? = null,
    onScrollToMessageConsumed: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val showTtl = !isExpired && expiresAt != null && expiresAt > 0L
    val view = LocalView.current
    var wasExpired by remember { mutableStateOf(isExpired) }
    val dissolve = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val timeline = remember(messages, nowMs) { buildRoomTimeline(messages, nowMs) }
    val ttlOffset = if (showTtl) 1 else 0
    val messageIndexById = remember(timeline) {
        buildMap {
            timeline.forEachIndexed { index, item ->
                if (item is RoomTimelineItem.Message) {
                    put(item.message.id, index)
                }
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) {
                true
            } else {
                val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
                last.index >= total - 1
            }
        }
    }
    var stickToBottom by remember { mutableStateOf(true) }
    var didInitialBottomScroll by remember { mutableStateOf(false) }

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

    LaunchedEffect(listState.isScrollInProgress, isAtBottom) {
        if (!listState.isScrollInProgress) {
            stickToBottom = isAtBottom
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        val shouldStickToLatest = messages.isNotEmpty() &&
            stickToBottom &&
            scrollToMessageId == null
        if (shouldStickToLatest) {
            val target = ttlOffset + timeline.lastIndex
            if (!didInitialBottomScroll) {
                listState.scrollToItem(target)
                didInitialBottomScroll = true
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    LaunchedEffect(scrollToMessageId) {
        val targetId = scrollToMessageId ?: return@LaunchedEffect
        val index = messageIndexById[targetId] ?: return@LaunchedEffect
        listState.animateScrollToItem(ttlOffset + index)
        onScrollToMessageConsumed()
    }

    val expiryProgress = roomExpiryProgress(expiresAt, activatedAt, nowMs)
    val showAura = !isExpired && shouldShowBubbleAura(expiryProgress)

    Box(modifier = modifier.fillMaxSize()) {
        RoomWallpaperLayer(wallpaperPath = wallpaperPath)
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = RoomUiDimens.spacingMedium),
                contentAlignment = Alignment.Center,
            ) {
                E2eEmptyCard(onClick = onOpenSafety)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = dissolve.value
                        scaleX = 0.96f + 0.04f * dissolve.value
                        scaleY = 0.96f + 0.04f * dissolve.value
                    },
                contentPadding = PaddingValues(
                    horizontal = RoomUiDimens.spacingMedium,
                    vertical = RoomUiDimens.spacingSmall,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (showTtl) {
                    val ttlAt = expiresAt!!
                    item(key = "ttl-chip") {
                        TtlChip(remainingMs = (ttlAt - nowMs).coerceAtLeast(0L))
                    }
                }
                items(
                    items = timeline,
                    key = { item ->
                        when (item) {
                            is RoomTimelineItem.DaySeparator -> "day-${item.dayStartMs}"
                            is RoomTimelineItem.Message -> item.message.id
                        }
                    },
                ) { item ->
                    when (item) {
                        is RoomTimelineItem.DaySeparator -> DaySeparatorChip(item)
                        is RoomTimelineItem.Message -> {
                            val msg = item.message
                            val parent = msg.replyToId?.let { id ->
                                messages.firstOrNull { it.id == id }
                            }
                            val replyQuote = msg.replyToId?.let {
                                when {
                                    parent == null -> ReplyQuoteUi(
                                        parentExists = false,
                                        snippet = "",
                                    )
                                    parent.recalled -> ReplyQuoteUi(
                                        parentExists = false,
                                        snippet = "",
                                    )
                                    parent.sensitive -> ReplyQuoteUi(
                                        parentExists = true,
                                        snippet = "••••",
                                    )
                                    else -> ReplyQuoteUi(
                                        parentExists = true,
                                        snippet = parent.body.take(80),
                                    )
                                }
                            }
                            val readReceipt = msg.direction == ChatMessage.DIRECTION_OUT &&
                                isMessageAtOrBeforeWatermark(msg.id, peerReadWatermarkId, messages)
                            Box(
                                modifier = (if (item.isGroupTail) {
                                    Modifier.padding(bottom = 6.dp)
                                } else {
                                    Modifier
                                }).then(
                                    if (msg.id == highlightMessageId) {
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                VanishXColors.Primary.copy(alpha = 0.18f),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .padding(vertical = 2.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                            ) {
                                RoomMessageBubble(
                                    message = msg,
                                    showAura = showAura,
                                    auraIntensity = expiryProgress,
                                    reactionCounts = reactionsByMessage[msg.id].orEmpty(),
                                    replyQuote = replyQuote,
                                    readReceipt = readReceipt,
                                    isGroupTail = item.isGroupTail,
                                    showTimestamp = item.showTimestamp,
                                    onLongPress = { onLongPressMessage(msg) },
                                    onMediaClick = { onMediaClick(msg) },
                                    onReplyQuoteClick = msg.replyToId?.let { id ->
                                        {
                                            val index = messageIndexById[id]
                                            if (index != null) {
                                                scope.launch {
                                                    stickToBottom = false
                                                    listState.animateScrollToItem(ttlOffset + index)
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isAtBottom,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VanishXColors.Surface2,
                    border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.45f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.clickable {
                        scope.launch {
                            listState.animateScrollToItem(ttlOffset + timeline.lastIndex)
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = VanishXColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.room_scroll_to_bottom),
                            style = MaterialTheme.typography.labelMedium,
                            color = VanishXColors.OnSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomWallpaperLayer(wallpaperPath: String?) {
    val wallpaper = wallpaperPath
    val colorArgb = RoomWallpaper.parseColorArgb(wallpaper)
    when {
        colorArgb != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(colorArgb)),
            )
        }
        !wallpaper.isNullOrBlank() -> {
            Image(
                painter = rememberAsyncImagePainter(File(wallpaper)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    if (RoomWallpaper.isBright(wallpaper) && !wallpaper.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VanishXColors.Bg.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun DaySeparatorChip(item: RoomTimelineItem.DaySeparator) {
    val label = when (item.kind) {
        DaySeparatorKind.TODAY -> stringResource(R.string.room_day_today)
        DaySeparatorKind.YESTERDAY -> stringResource(R.string.room_day_yesterday)
        DaySeparatorKind.DATE -> formatDaySeparatorDate(item.dayStartMs)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VanishXColors.Surface2.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, VanishXColors.GlassBorder.copy(alpha = 0.35f)),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.Muted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
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
    reactionsByMessage: Map<String, Map<String, Int>> = emptyMap(),
    peerReadWatermarkId: String? = null,
    onLongPressMessage: (ChatMessage) -> Unit = {},
    onMediaClick: (ChatMessage) -> Unit = {},
    scrollToMessageId: String? = null,
    highlightMessageId: String? = null,
    wallpaperPath: String? = null,
    onScrollToMessageConsumed: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoomMessageList(
            messages = messages,
            expiresAt = expiresAt,
            activatedAt = activatedAt,
            isExpired = isExpired,
            onOpenSafety = onOpenSafety,
            reactionsByMessage = reactionsByMessage,
            peerReadWatermarkId = peerReadWatermarkId,
            onLongPressMessage = onLongPressMessage,
            onMediaClick = onMediaClick,
            scrollToMessageId = scrollToMessageId,
            highlightMessageId = highlightMessageId,
            wallpaperPath = wallpaperPath,
            onScrollToMessageConsumed = onScrollToMessageConsumed,
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
