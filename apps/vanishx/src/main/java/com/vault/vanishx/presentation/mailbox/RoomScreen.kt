@file:Suppress("TooManyFunctions", "ComplexMethod", "ComplexCondition", "ModifierMissing")

package com.vault.vanishx.presentation.mailbox

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.mailbox.chat.AttachTraySheet
import com.vault.vanishx.presentation.mailbox.chat.FeedbackMessages
import com.vault.vanishx.presentation.mailbox.chat.REPORT_REASON_MAX_LINES
import com.vault.vanishx.presentation.mailbox.chat.RoomActiveBody
import com.vault.vanishx.presentation.mailbox.chat.RoomBentoSheet
import com.vault.vanishx.presentation.mailbox.chat.RoomComposer
import com.vault.vanishx.presentation.mailbox.chat.RoomExpired
import com.vault.vanishx.presentation.mailbox.chat.RoomExpiredFree
import com.vault.vanishx.presentation.mailbox.chat.RoomExpiredProArchive
import com.vault.vanishx.presentation.mailbox.chat.RoomHeader
import com.vault.vanishx.presentation.mailbox.chat.RoomInviteSheet
import com.vault.vanishx.presentation.mailbox.chat.RoomLeft
import com.vault.vanishx.presentation.mailbox.chat.RoomLoading
import com.vault.vanishx.presentation.mailbox.chat.RoomMessageActionSheet
import com.vault.vanishx.presentation.mailbox.chat.MediaViewerDialog
import com.vault.vanishx.presentation.mailbox.chat.RoomMediaLibraryScreen
import com.vault.vanishx.presentation.mailbox.chat.RoomOptionsScreen
import com.vault.vanishx.presentation.mailbox.chat.RoomSearchBar
import com.vault.vanishx.presentation.mailbox.chat.RoomSafetySheet
import com.vault.vanishx.presentation.mailbox.chat.RoomScreenshotBanner
import com.vault.vanishx.presentation.mailbox.chat.RoomUiDimens
import com.vault.vanishx.presentation.mailbox.chat.RoomWallpaperSheet
import com.vault.vanishx.presentation.mailbox.chat.VoiceRecordTray
import com.vault.vanishx.presentation.mailbox.chat.WaitingStage
import com.vault.vanishx.presentation.mailbox.chat.formatMessageTime
import com.vault.vanishx.presentation.mailbox.chat.isMessageAtOrBeforeWatermark
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

@Composable
fun RoomScreen(
    viewModel: RoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showVoiceTray by remember { mutableStateOf(false) }
    val appLockSession = remember(context) {
        EntryPointAccessors.fromActivity(
            context as Activity,
            AppLockEntryPoint::class.java,
        ).appLockSession()
    }
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        appLockSession.endExternalUi()
        if (uri != null) {
            onAttachmentSelected(
                context = context,
                uri = uri,
                onAction = viewModel::onAction,
            )
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        appLockSession.endExternalUi()
        if (uri != null) {
            viewModel.onAction(RoomAction.SetRoomAvatar(uri))
        }
    }
    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        appLockSession.endExternalUi()
        if (uri != null) {
            viewModel.onAction(RoomAction.SetRoomWallpaper(uri))
        }
    }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        appLockSession.endExternalUi()
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onAttachmentSelected(
                context = context,
                uri = uri,
                onAction = viewModel::onAction,
            )
        }
    }
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    LaunchedEffect(uiState.pingPeerEvent) {
        val message = when (val event = uiState.pingPeerEvent) {
            is PingPeerEvent.Sent -> context.getString(R.string.room_ping_peer_sent)
            is PingPeerEvent.Cooldown ->
                context.getString(R.string.room_ping_peer_cooldown, event.secondsRemaining)
            null -> null
        }
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onAction(RoomAction.ConsumePingPeerEvent)
        }
    }

    LaunchedEffect(uiState.pendingClipboard) {
        val text = uiState.pendingClipboard ?: return@LaunchedEffect
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VanishX message", text))
        viewModel.onAction(RoomAction.ConsumeClipboard)
    }

    LaunchedEffect(uiState.toastMessage) {
        val key = uiState.toastMessage ?: return@LaunchedEffect
        val message = when (key) {
            "peer_joined" -> context.getString(R.string.room_peer_joined)
            "sensitive_copy_blocked" -> context.getString(R.string.room_copy_blocked_sensitive)
            "media_saved" -> context.getString(R.string.room_media_saved)
            "media_save_failed" -> context.getString(R.string.room_media_save_failed)
            else -> key
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.onAction(RoomAction.ConsumeToast)
    }

    ScreenCaptureEffect(
        enabled = !uiState.isLoading && uiState.room != null,
        onCaptured = { viewModel.onAction(RoomAction.ScreenshotDetected) },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RoomContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onCopyInvite = { uri ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("VanishX invite", uri))
                Toast.makeText(context, context.getString(R.string.create_copied), Toast.LENGTH_SHORT).show()
            },
            onShareInvite = { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, uri)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.create_share)))
            },
            onOpenMore = {
                viewModel.onAction(RoomAction.AttachClicked)
            },
            onOpenVoice = { showVoiceTray = true },
            onPickGallery = {
                appLockSession.beginExternalUi()
                galleryPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            },
            onPickDocument = {
                appLockSession.beginExternalUi()
                documentPicker.launch(
                    arrayOf(
                        "application/pdf",
                        "text/plain",
                        "text/markdown",
                        "text/x-markdown",
                        "application/zip",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "*/*",
                    ),
                )
            },
        )
        if (uiState.showRoomOptions) {
            RoomOptionsScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                onPickAvatar = {
                    appLockSession.beginExternalUi()
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
        if (uiState.showMediaLibrary) {
            RoomMediaLibraryScreen(
                messages = uiState.messages,
                onBack = { viewModel.onAction(RoomAction.DismissMediaLibrary) },
                onOpenMessage = { id ->
                    viewModel.onAction(RoomAction.DismissMediaLibrary)
                    viewModel.onAction(RoomAction.OpenMediaViewer(id))
                },
            )
        }
    }

    if (uiState.showWallpaperSheet) {
        RoomWallpaperSheet(
            currentToken = uiState.room?.wallpaperLocalPath,
            onPickPreset = { token -> viewModel.onAction(RoomAction.SetWallpaperPreset(token)) },
            onPickGallery = {
                viewModel.onAction(RoomAction.DismissWallpaperSheet)
                appLockSession.beginExternalUi()
                wallpaperPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onReset = { viewModel.onAction(RoomAction.ResetRoomWallpaper) },
            onDismiss = { viewModel.onAction(RoomAction.DismissWallpaperSheet) },
        )
    }

    if (showVoiceTray) {
        VoiceRecordTray(onDismiss = { showVoiceTray = false })
    }
}

@Composable
private fun RoomContent(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    onOpenMore: () -> Unit,
    onOpenVoice: () -> Unit,
    onPickGallery: () -> Unit,
    onPickDocument: () -> Unit,
) {
    val showComposer = !uiState.isLoading &&
        uiState.room?.status == MailboxRoom.STATUS_ACTIVE &&
        !uiState.isExpired
    val roomHandshakeStatus = handshakeStatus(uiState.room, uiState.isExpired)
    val isHandshakeWaiting = roomHandshakeStatus == RoomHandshakeStatus.WAITING
    val inviteUri = uiState.room?.let { room ->
        RoomInvite(
            roomId = room.id,
            roomKey = room.roomKey,
            expiresAt = room.expiresAt.takeIf { it > 0L },
        ).toUriString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        RoomHeader(
            uiState = uiState,
            onAction = onAction,
            handshakeStatus = roomHandshakeStatus,
        )

        if (uiState.showRoomSearch) {
            RoomSearchBar(uiState = uiState, onAction = onAction)
        }

        if (uiState.showScreenshotBanner) {
            RoomScreenshotBanner(
                onDismiss = { onAction(RoomAction.DismissScreenshotBanner) },
            )
        }

        when {
            uiState.isLoading -> RoomLoading(modifier = Modifier.weight(1f))
            uiState.room?.status == MailboxRoom.STATUS_LEFT -> RoomLeft(modifier = Modifier.weight(1f))
            uiState.isExpired && !uiState.isPro -> RoomExpiredFree(
                modifier = Modifier.weight(1f),
                onAction = onAction,
            )
            uiState.isExpired && uiState.isPro -> RoomExpiredProArchive(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
            uiState.isExpired -> RoomExpired(modifier = Modifier.weight(1f))
            isHandshakeWaiting -> WaitingStage(
                room = uiState.room,
                inviteUri = inviteUri,
                onAction = onAction,
                onCopyInvite = onCopyInvite,
                onShareInvite = onShareInvite,
                modifier = Modifier.weight(1f),
            )
            else -> RoomActiveBody(
                messages = uiState.messages,
                expiresAt = uiState.room?.expiresAt?.takeIf { uiState.room?.hasRoomClock() == true },
                activatedAt = uiState.room?.activatedAt,
                isExpired = uiState.isExpired,
                isSyncing = uiState.isSyncing,
                onOpenSafety = { onAction(RoomAction.OpenSafetySheet) },
                reactionsByMessage = uiState.reactionsByMessage,
                peerReadWatermarkId = uiState.peerReadWatermarkId,
                onLongPressMessage = { onAction(RoomAction.OpenMessageActions(it.id)) },
                onMediaClick = { msg ->
                    when (msg.mediaTransferStatus) {
                        ChatMessage.MEDIA_FAILED -> onAction(RoomAction.DismissFailedMedia)
                        ChatMessage.MEDIA_PENDING -> Unit
                        else -> onAction(RoomAction.OpenMediaViewer(msg.id))
                    }
                },
                scrollToMessageId = uiState.scrollToMessageId,
                highlightMessageId = uiState.highlightMessageId,
                wallpaperPath = uiState.room?.wallpaperLocalPath,
                onScrollToMessageConsumed = { onAction(RoomAction.ConsumeScrollToMessage) },
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)

        if (showComposer) {
            val replySnippet = uiState.replyToMessageId?.let { id ->
                uiState.messages.firstOrNull { it.id == id }?.let { msg ->
                    when {
                        msg.recalled -> "…"
                        msg.sensitive -> "••••"
                        else -> msg.body
                    }
                }
            }
            RoomComposer(
                draft = uiState.draft,
                isSending = uiState.isSending,
                isSendingMedia = uiState.isSendingMedia,
                onOpenMore = onOpenMore,
                onOpenVoice = onOpenVoice,
                onPickGallery = onPickGallery,
                locked = isHandshakeWaiting,
                replySnippet = replySnippet,
                onAction = onAction,
            )
        }
    }

    if (uiState.showAttachTray) {
        AttachTraySheet(
            enabled = !uiState.isSendingMedia,
            onPickDocument = onPickDocument,
            onAction = onAction,
        )
    }

    if (uiState.showSensitiveSendConfirm) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_sensitive_send_title),
            body = stringResource(R.string.room_sensitive_send_body),
            confirmLabel = stringResource(R.string.room_sensitive_send_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Warn,
            onConfirm = { onAction(RoomAction.ConfirmSensitiveSend) },
            onDismiss = { onAction(RoomAction.DismissSensitiveSend) },
        )
    }

    if (uiState.showBlockConfirm) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_block_title),
            body = stringResource(R.string.room_block_body),
            confirmLabel = stringResource(R.string.room_block_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Danger,
            onConfirm = { onAction(RoomAction.ConfirmBlock) },
            onDismiss = { onAction(RoomAction.DismissBlockConfirm) },
        )
    }

    if (uiState.showBurnConfirm) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_burn_title),
            body = stringResource(R.string.room_burn_body),
            confirmLabel = stringResource(R.string.room_burn_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Danger,
            onConfirm = { onAction(RoomAction.ConfirmBurn) },
            onDismiss = { onAction(RoomAction.DismissBurnConfirm) },
        )
    }

    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissRenameDialog) },
            title = { Text(text = stringResource(R.string.room_rename_title)) },
            text = {
                OutlinedTextField(
                    value = uiState.renameDraft,
                    onValueChange = { onAction(RoomAction.RenameDraftChanged(it)) },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.room_rename_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(RoomAction.ConfirmRename) }) {
                    Text(text = stringResource(R.string.room_rename_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(RoomAction.DismissRenameDialog) }) {
                    Text(text = stringResource(R.string.action_back))
                }
            },
        )
    }

    if (uiState.showReportDialog) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissReport) },
            title = { Text(text = stringResource(R.string.room_report_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.room_report_body))
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
                    OutlinedTextField(
                        value = uiState.reportReason,
                        onValueChange = { onAction(RoomAction.ReportReasonChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(text = stringResource(R.string.room_report_reason_hint))
                        },
                        maxLines = REPORT_REASON_MAX_LINES,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(RoomAction.SubmitReport) },
                    enabled = !uiState.isReporting,
                ) {
                    Text(text = stringResource(R.string.room_report_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(RoomAction.DismissReport) }) {
                    Text(text = stringResource(R.string.action_back))
                }
            },
        )
    }

    if (uiState.showPingConfirm) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_ping_title),
            body = stringResource(R.string.room_ping_body),
            confirmLabel = stringResource(R.string.room_ping_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Accent,
            onConfirm = { onAction(RoomAction.PingRoom) },
            onDismiss = { onAction(RoomAction.DismissPing) },
        )
    }

    if (uiState.showInviteSheet && inviteUri != null) {
        RoomInviteSheet(
            inviteUri = inviteUri,
            onCopy = onCopyInvite,
            onShare = onShareInvite,
            onDismiss = { onAction(RoomAction.DismissInviteSheet) },
        )
    }

    if (uiState.showBentoSheet) {
        RoomBentoSheet(uiState = uiState, onAction = onAction)
    }

    if (uiState.showSafetySheet) {
        RoomSafetySheet(
            roomKey = uiState.room?.roomKey.orEmpty(),
            onDismiss = { onAction(RoomAction.DismissSafetySheet) },
        )
    }

    val actionMessage = uiState.actionMessageId?.let { id ->
        uiState.messages.firstOrNull { it.id == id }
    }
    val mediaViewerMessage = uiState.mediaViewerMessageId?.let { id ->
        uiState.messages.firstOrNull { it.id == id && it.isMedia }
    }
    if (mediaViewerMessage != null) {
        MediaViewerDialog(
            message = mediaViewerMessage,
            isPro = uiState.isPro,
            onDismiss = { onAction(RoomAction.DismissMediaViewer) },
            onSave = {
                onAction(RoomAction.SaveMedia(mediaViewerMessage.id))
            },
        )
    }
    if (actionMessage != null) {
        RoomMessageActionSheet(
            uiState = uiState,
            message = actionMessage,
            onAction = onAction,
        )
    }

    if (uiState.deleteConfirmMessageId != null) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_delete_title),
            body = stringResource(R.string.room_delete_body),
            confirmLabel = stringResource(R.string.room_delete_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Danger,
            onConfirm = { onAction(RoomAction.ConfirmDeleteForMe) },
            onDismiss = { onAction(RoomAction.DismissDeleteForMe) },
        )
    }

    val detailsMessage = uiState.detailsMessageId?.let { id ->
        uiState.messages.firstOrNull { it.id == id }
    }
    if (detailsMessage != null) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissMessageDetails) },
            title = { Text(text = stringResource(R.string.room_action_details)) },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.room_details_sent_at,
                            formatMessageTime(detailsMessage.sentAt),
                        ),
                    )
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
                    Text(
                        text = stringResource(
                            when {
                                detailsMessage.direction !=
                                    com.vault.vanishx.domain.model.ChatMessage.DIRECTION_OUT ->
                                    R.string.room_details_status_received
                                isMessageAtOrBeforeWatermark(
                                    detailsMessage.id,
                                    uiState.peerReadWatermarkId,
                                    uiState.messages,
                                ) -> R.string.room_details_status_read
                                else -> R.string.room_details_status_sent
                            },
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(RoomAction.DismissMessageDetails) }) {
                    Text(text = stringResource(R.string.room_safety_close))
                }
            },
        )
    }
}

private fun onAttachmentSelected(
    context: Context,
    uri: android.net.Uri,
    onAction: (RoomAction) -> Unit,
) {
    val mime = resolveAttachmentMime(context, uri)
    val displayName = resolveAttachmentDisplayName(context, uri)
    onAction(RoomAction.SendMedia(uri, mime, displayName))
}

private fun resolveAttachmentDisplayName(context: Context, uri: android.net.Uri): String? {
    val fromQuery = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index < 0) null else cursor.getString(index)
        }
    }.getOrNull()
    return fromQuery ?: uri.lastPathSegment
}

private fun resolveAttachmentMime(context: Context, uri: android.net.Uri): String {
    context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }
    val name = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index < 0) null else cursor.getString(index)
        }
    }.getOrNull()
    val fromName = name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
    val fromPath = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
    val ext = fromName ?: fromPath
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "3gp", "3gpp" -> "video/3gpp"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "md", "markdown" -> "text/markdown"
        "zip" -> "application/zip"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "application/octet-stream"
    }
}

@Composable
private fun ScreenCaptureEffect(
    enabled: Boolean,
    onCaptured: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return@DisposableEffect onDispose { }
        }
        val activity = context.findActivity() ?: return@DisposableEffect onDispose { }
        val callback = Activity.ScreenCaptureCallback { onCaptured() }
        val registered = runCatching {
            activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
        }.onFailure { e ->
            Timber.w(e, "Screen capture callback unavailable")
        }.isSuccess
        onDispose {
            if (registered) {
                runCatching { activity.unregisterScreenCaptureCallback(callback) }
            }
        }
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
