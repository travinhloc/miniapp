@file:Suppress("MagicNumber", "LongMethod", "UnstableCollections")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

/**
 * Full-screen media viewer: ViewPager2 + PhotoView · swipe-down dismiss · reactions.
 */
@Composable
internal fun PhotoViewerScreen(
    pages: List<MediaViewerPage>,
    initialPageIndex: Int,
    room: MailboxRoom?,
    reactionsByMessage: Map<String, Map<String, Int>>,
    myReactionByMessage: Map<String, String>,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onSave: (messageId: String) -> Unit,
    onAction: (RoomAction) -> Unit,
) {
    if (pages.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val startIndex = initialPageIndex.coerceIn(0, pages.lastIndex)
    var currentPageIndex by remember(pages, startIndex) { mutableIntStateOf(startIndex) }
    var chromeVisible by remember { mutableStateOf(true) }
    val dismissOffsetState = remember { mutableFloatStateOf(0f) }
    var dismissOffset by dismissOffsetState
    val dismissThresholdPx = with(LocalDensity.current) { PHOTO_VIEWER_DISMISS_THRESHOLD_DP.dp.toPx() }
    val dismissProgress = (dismissOffset / dismissThresholdPx).coerceIn(0f, 1f)

    val currentPage = pages[currentPageIndex]
    val currentMessageId = currentPage.messageId
    val peerTitle = resolveRoomTitle(room).ifBlank {
        stringResource(R.string.room_rename_placeholder)
    }
    val senderTitle = if (currentPage.direction == ChatMessage.DIRECTION_OUT) {
        stringResource(R.string.room_viewer_you)
    } else {
        peerTitle
    }
    val senderAvatarPath = if (currentPage.direction == ChatMessage.DIRECTION_OUT) {
        null
    } else {
        room?.avatarLocalPath
    }
    val senderLetter = if (currentPage.direction == ChatMessage.DIRECTION_OUT) {
        stringResource(R.string.room_viewer_you).firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    } else {
        resolveAvatarLetter(peerTitle)
    }
    val reactionCounts = reactionsByMessage[currentMessageId].orEmpty()
    val myReaction = myReactionByMessage[currentMessageId]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .vanishxScreenInsets()
            .graphicsLayer {
                translationY = dismissOffset
                alpha = 1f - dismissProgress * 0.45f
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PhotoViewerPagerLayout(context)
            },
            update = { layout ->
                layout.setCallbacks(
                    PhotoViewerPagerLayout.Callbacks(
                        onDismissOffsetChanged = { offset ->
                            dismissOffsetState.floatValue = offset
                        },
                        onDismissRequested = onDismiss,
                        onPageSelected = { index ->
                            currentPageIndex = index
                            dismissOffsetState.floatValue = 0f
                        },
                        onPhotoTap = { chromeVisible = !chromeVisible },
                    ),
                )
                layout.setContent(pages, startIndex)
            },
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            PhotoViewerTopBar(
                senderTitle = senderTitle,
                senderLetter = senderLetter,
                avatarPath = senderAvatarPath,
                pageLabel = "${currentPageIndex + 1}/${pages.size}",
                isPro = isPro,
                onBack = onDismiss,
                onSave = { onSave(currentMessageId) },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PhotoViewerBottomChrome(
                reactionCounts = reactionCounts,
                myReaction = myReaction,
                sentAt = currentPage.sentAt,
                onPickReaction = { emoji ->
                    onAction(RoomAction.ReactToMessage(currentMessageId, emoji))
                },
            )
        }
    }
}

@Composable
private fun PhotoViewerTopBar(
    senderTitle: String,
    senderLetter: String,
    avatarPath: String?,
    pageLabel: String,
    isPro: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = Color.White,
            )
        }
        RoomAvatar(
            letter = senderLetter,
            size = 36.dp,
            imagePath = avatarPath,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = senderTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pageLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
        TextButton(onClick = onSave) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = stringResource(R.string.room_media_save),
                    tint = Color.White,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = stringResource(
                        if (isPro) R.string.room_media_save else R.string.room_media_save_pro,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PhotoViewerBottomChrome(
    reactionCounts: Map<String, Int>,
    myReaction: String?,
    sentAt: Long,
    onPickReaction: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (reactionCounts.isNotEmpty()) {
            ViewerReactionCountsRow(counts = reactionCounts)
        }
        ReactionPickerBar(
            selected = myReaction,
            onPick = onPickReaction,
        )
        Text(
            text = formatMessageTime(sentAt),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun ViewerReactionCountsRow(counts: Map<String, Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        counts.entries
            .sortedByDescending { it.value }
            .forEach { (emoji, count) ->
                Row(
                    modifier = Modifier
                        .background(
                            VanishXColors.Surface2.copy(alpha = 0.85f),
                            androidx.compose.foundation.shape.RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = emoji, fontSize = 16.sp)
                    if (count > 1) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = VanishXColors.OnSurface,
                        )
                    }
                }
            }
    }
}
