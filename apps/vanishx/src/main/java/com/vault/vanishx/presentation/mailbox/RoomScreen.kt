@file:Suppress(
    "TooManyFunctions",
    "ComplexMethod",
    "ComplexCondition",
    "ModifierMissing",
    "ReturnCount",
    "MagicNumber",
)

package com.vault.vanishx.presentation.mailbox

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.vault.vanishx.presentation.util.appDetailsSettingsIntent
import androidx.compose.runtime.saveable.rememberSaveable
import android.provider.Settings
import android.net.Uri
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.MediaAlbumMerge
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.data.media.ImagePrepareHelper
import com.vault.vanishx.data.media.VideoPrepareHelper
import com.vault.vanishx.data.media.VoicePlaybackBus
import com.vault.vanishx.data.media.VoiceRecorder
import com.vault.vanishx.presentation.mailbox.chat.AttachTraySheet
import com.vault.vanishx.presentation.mailbox.chat.CameraCaptureScreen
import com.vault.vanishx.presentation.mailbox.chat.CameraCaptureTab
import com.vault.vanishx.presentation.mailbox.chat.DocumentViewerScreen
import com.vault.vanishx.presentation.mailbox.chat.GalleryAttachSheet
import com.vault.vanishx.presentation.mailbox.chat.GalleryLibraryScreen
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
import com.vault.vanishx.presentation.mailbox.chat.PhotoViewerScreen
import com.vault.vanishx.presentation.mailbox.chat.buildMediaViewerPages
import com.vault.vanishx.presentation.mailbox.chat.findMediaViewerPageIndex
import com.vault.vanishx.presentation.mailbox.chat.RoomMediaLibraryScreen
import com.vault.vanishx.presentation.mailbox.chat.RoomOptionsScreen
import com.vault.vanishx.presentation.mailbox.chat.RoomSearchBar
import com.vault.vanishx.presentation.mailbox.chat.RoomSafetySheet
import com.vault.vanishx.presentation.mailbox.chat.RoomScreenshotBanner
import com.vault.vanishx.presentation.mailbox.chat.RoomUiDimens
import com.vault.vanishx.presentation.mailbox.chat.RoomWallpaperSheet
import com.vault.vanishx.presentation.mailbox.chat.VoiceRecordSheet
import com.vault.vanishx.presentation.mailbox.chat.WaitingStage
import com.vault.vanishx.presentation.mailbox.chat.formatMessageTime
import com.vault.vanishx.presentation.mailbox.chat.isMessageAtOrBeforeWatermark
import com.vault.vanishx.presentation.mailbox.chat.resolveRoomTitle
import com.vault.vanishx.presentation.theme.VanishXColors
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RoomScreen(
    viewModel: RoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun dismissRoomKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    var showCameraCapture by remember { mutableStateOf(false) }
    var showGallerySheet by remember { mutableStateOf(false) }
    var showGalleryLibrary by remember { mutableStateOf(false) }
    var cameraInitialTab by remember { mutableStateOf(CameraCaptureTab.Photo) }
    var showVoicePermission by remember { mutableStateOf(false) }
    var voicePermissionRequested by rememberSaveable { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    val voicePermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val voiceRecorder = remember(context) { VoiceRecorder(context.applicationContext) }
    val imagePrepareHelper = remember(context) {
        ImagePrepareHelper(context.applicationContext)
    }
    val videoPrepareHelper = remember(context) {
        VideoPrepareHelper(context.applicationContext)
    }
    val appLockSession = remember(context) {
        EntryPointAccessors.fromActivity(
            context as Activity,
            AppLockEntryPoint::class.java,
        ).appLockSession()
    }

    val appSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        appLockSession.endExternalUi()
    }

    DisposableEffect(voiceRecorder) {
        onDispose {
            VoicePlaybackBus.stop()
            voiceRecorder.release()
        }
    }

    fun openVoiceSheet() {
        if (uiState.isSendingMedia || showVoiceSheet) return
        if (!voicePermission.status.isGranted) {
            showVoicePermission = true
            return
        }
        showVoiceSheet = true
    }
    fun sendGallerySelection(uris: List<android.net.Uri>) {
        val items = MediaLimits.clampPhotoMultiSelect(uris).map { uri ->
            RoomAction.SendMedia(
                uri = uri,
                mime = resolveAttachmentMime(context, uri),
                displayName = resolveAttachmentDisplayName(context, uri),
            )
        }
        viewModel.onAction(RoomAction.SendMediaQueue(items))
    }

    fun gallerySelectionLimitToast() {
        Toast.makeText(
            context,
            context.getString(
                R.string.room_gallery_max_selected,
                MediaLimits.PHOTO_MULTI_SELECT_MAX,
            ),
            Toast.LENGTH_SHORT,
        ).show()
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
    viewModel.navigator.collectAsEffect { destination ->
        dismissRoomKeyboard()
        navigator(destination)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.roomId) {
        val observer = LifecycleEventObserver { _, event ->
            viewModel.onRoomLifecycle(event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onRoomLifecycle(Lifecycle.Event.ON_PAUSE)
        }
    }

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
        val message = when {
            key == "peer_joined" -> context.getString(R.string.room_peer_joined)
            key == "sensitive_copy_blocked" -> context.getString(R.string.room_copy_blocked_sensitive)
            key == "media_saved" -> context.getString(R.string.room_media_saved)
            key == "media_save_failed" -> context.getString(R.string.room_media_save_failed)
            key.startsWith("media_sending|") -> {
                val parts = key.split('|')
                val current = parts.getOrNull(1)?.toIntOrNull()
                val total = parts.getOrNull(2)?.toIntOrNull()
                if (current != null && total != null) {
                    context.getString(R.string.room_media_sending_progress, current, total)
                } else {
                    context.getString(R.string.room_media_uploading)
                }
            }
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
            dispatchAction = viewModel::onAction,
            onTapOutsideComposer = ::dismissRoomKeyboard,
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
                dismissRoomKeyboard()
                viewModel.onAction(RoomAction.AttachClicked)
            },
            onOpenVoice = {
                dismissRoomKeyboard()
                openVoiceSheet()
            },
            onPickGallery = {
                dismissRoomKeyboard()
                showGallerySheet = true
            },
            onPickDocument = {
                dismissRoomKeyboard()
                appLockSession.beginExternalUi()
                documentPicker.launch(MediaLimits.DOCUMENT_PICKER_MIME)
            },
            onOpenCamera = {
                dismissRoomKeyboard()
                cameraInitialTab = CameraCaptureTab.Photo
                showCameraCapture = true
            },
        )
        if (showVoiceSheet) {
            VoiceRecordSheet(
                voiceRecorder = voiceRecorder,
                enabled = !uiState.isSendingMedia,
                onDismiss = { showVoiceSheet = false },
                onSend = { uri, displayName ->
                    viewModel.onAction(
                        RoomAction.SendMedia(
                            uri = uri,
                            mime = "audio/mp4",
                            displayName = displayName,
                        ),
                    )
                },
                onRecordFailed = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.room_voice_record_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onTooShort = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.room_voice_too_short),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }
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

    if (showVoicePermission) {
        val permanentlyDenied = !voicePermission.status.isGranted &&
            !voicePermission.status.shouldShowRationale &&
            voicePermissionRequested
        VoicePermissionDialog(
            showRationale = voicePermission.status.shouldShowRationale,
            permanentlyDenied = permanentlyDenied,
            onAllow = {
                showVoicePermission = false
                if (permanentlyDenied) {
                    appLockSession.beginExternalUi()
                    appSettingsLauncher.launch(appDetailsSettingsIntent(context))
                } else {
                    voicePermissionRequested = true
                    voicePermission.launchPermissionRequest()
                }
            },
            onDismiss = { showVoicePermission = false },
        )
    }
    if (showGallerySheet) {
        GalleryAttachSheet(
            enabled = !uiState.isSendingMedia,
            onDismiss = { showGallerySheet = false },
            onTakePhoto = {
                cameraInitialTab = CameraCaptureTab.Photo
                showCameraCapture = true
            },
            onOpenLibrary = { showGalleryLibrary = true },
            onSendSelected = { uris ->
                sendGallerySelection(uris)
            },
            onSelectionLimitReached = { gallerySelectionLimitToast() },
        )
    }
    if (showGalleryLibrary) {
        val recipient = resolveRoomTitle(uiState.room).ifBlank {
            context.getString(R.string.room_rename_placeholder)
        }
        GalleryLibraryScreen(
            recipientName = recipient,
            enabled = !uiState.isSendingMedia,
            onBack = { showGalleryLibrary = false },
            onTakePhoto = {
                showGalleryLibrary = false
                cameraInitialTab = CameraCaptureTab.Photo
                showCameraCapture = true
            },
            onSendSelected = { uris ->
                showGalleryLibrary = false
                sendGallerySelection(uris)
            },
            onSelectionLimitReached = { gallerySelectionLimitToast() },
        )
    }
    if (showCameraCapture) {
        CameraCaptureScreen(
            imagePrepareHelper = imagePrepareHelper,
            videoPrepareHelper = videoPrepareHelper,
            onDismiss = { showCameraCapture = false },
            initialTab = cameraInitialTab,
            onPhotoReady = { uri, displayName ->
                showCameraCapture = false
                viewModel.onAction(
                    RoomAction.SendMedia(uri, "image/jpeg", displayName),
                )
            },
            onVideoReady = { uri, displayName ->
                showCameraCapture = false
                viewModel.onAction(
                    RoomAction.SendMedia(uri, "video/mp4", displayName),
                )
            },
        )
    }
}

@Composable
private fun RoomContent(
    uiState: RoomUiState,
    dispatchAction: (RoomAction) -> Unit,
    onTapOutsideComposer: () -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    onOpenMore: () -> Unit,
    onOpenVoice: () -> Unit,
    onPickGallery: () -> Unit,
    onPickDocument: () -> Unit,
    onOpenCamera: () -> Unit,
) {
    val onAction: (RoomAction) -> Unit = { action ->
        if (action.opensAnotherRoomSurface()) onTapOutsideComposer()
        dispatchAction(action)
    }
    var showNeedPro by remember { mutableStateOf(false) }
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
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
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
                onNeedPro = { showNeedPro = true },
                onAction = onAction,
                modifier = Modifier.weight(1f),
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
                onMediaClick = { msg, albumIndex ->
                    when {
                        msg.mediaKind == AttachmentMeta.KIND_VOICE -> Unit
                        msg.mediaTransferStatus == ChatMessage.MEDIA_FAILED &&
                            msg.mediaKind != AttachmentMeta.KIND_ALBUM ->
                            onAction(RoomAction.DismissFailedMedia)
                        else -> onAction(RoomAction.OpenMediaViewer(msg.id, albumIndex))
                    }
                },
                albumsById = MediaAlbumMerge.resolveAlbums(
                    uiState.messages,
                    uiState.outgoingAlbums,
                ).associateBy { it.id },
                scrollToMessageId = uiState.scrollToMessageId,
                highlightMessageId = uiState.highlightMessageId,
                wallpaperPath = uiState.room?.wallpaperLocalPath,
                onScrollToMessageConsumed = { onAction(RoomAction.ConsumeScrollToMessage) },
                onTapOutsideComposer = onTapOutsideComposer,
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)

        val attachErrorRes = when (uiState.errorMessage) {
            "media_unsupported" -> R.string.room_media_unsupported_type
            "media_too_large" -> R.string.room_media_too_large
            else -> null
        }
        if (attachErrorRes != null) {
            VanishXAlertDialog(
                title = stringResource(R.string.room_attach_error_title),
                body = stringResource(attachErrorRes),
                confirmLabel = stringResource(R.string.action_back),
                tone = VanishXAlertTone.Warn,
                onConfirm = { onAction(RoomAction.ClearFeedback) },
                onDismiss = { onAction(RoomAction.ClearFeedback) },
            )
        }

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
            onOpenCamera = onOpenCamera,
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
        VanishXAlertDialog(
            title = stringResource(R.string.room_rename_title),
            body = stringResource(R.string.room_rename_hint),
            confirmLabel = stringResource(R.string.room_rename_save),
            dismissLabel = stringResource(R.string.action_back),
            confirmEnabled = uiState.renameDraft.isNotBlank(),
            onConfirm = { onAction(RoomAction.ConfirmRename) },
            onDismiss = { onAction(RoomAction.DismissRenameDialog) },
            extraContent = {
                OutlinedTextField(
                    value = uiState.renameDraft,
                    onValueChange = { onAction(RoomAction.RenameDraftChanged(it)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }

    if (uiState.showReportDialog) {
        VanishXAlertDialog(
            title = stringResource(R.string.room_report_title),
            body = stringResource(R.string.room_report_body),
            confirmLabel = stringResource(R.string.room_report_submit),
            dismissLabel = stringResource(R.string.action_back),
            confirmEnabled = !uiState.isReporting,
            onConfirm = { onAction(RoomAction.SubmitReport) },
            onDismiss = { onAction(RoomAction.DismissReport) },
            extraContent = {
                OutlinedTextField(
                    value = uiState.reportReason,
                    onValueChange = { onAction(RoomAction.ReportReasonChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = stringResource(R.string.room_report_reason_hint))
                    },
                    maxLines = REPORT_REASON_MAX_LINES,
                )
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
        val msg = uiState.messages.firstOrNull { it.id == id && it.isMedia } ?: return@let null
        if (msg.mediaKind == AttachmentMeta.KIND_ALBUM) {
            val album = uiState.outgoingAlbums.firstOrNull { it.id == id } ?: return@let null
            val index = uiState.mediaViewerAlbumIndex ?: 0
            val item = album.items.getOrNull(index) ?: return@let null
            ChatMessage(
                id = item.sentMessageId ?: "${msg.id}_$index",
                roomId = msg.roomId,
                body = "",
                sentAt = msg.sentAt,
                expiresAt = msg.expiresAt,
                direction = msg.direction,
                mediaKind = item.kind,
                mediaMime = item.mime,
                mediaFileName = item.displayName,
                mediaLocalPath = item.previewPath,
                mediaTransferStatus = item.status,
            )
        } else {
            msg
        }
    }
    if (mediaViewerMessage != null) {
        // Overlay is not a Nav destination — intercept system Back so we don't leave the room.
        BackHandler { onAction(RoomAction.DismissMediaViewer) }
        when (mediaViewerMessage.mediaKind) {
            AttachmentMeta.KIND_FILE -> {
                DocumentViewerScreen(
                    message = mediaViewerMessage,
                    isPro = uiState.isPro,
                    onBack = { onAction(RoomAction.DismissMediaViewer) },
                    onSave = {
                        if (uiState.isPro) {
                            onAction(RoomAction.SaveMedia(mediaViewerMessage.id))
                        } else {
                            showNeedPro = true
                        }
                    },
                )
            }
            AttachmentMeta.KIND_IMAGE, AttachmentMeta.KIND_VIDEO -> {
                val albums = MediaAlbumMerge.resolveAlbums(uiState.messages, uiState.outgoingAlbums)
                val focusedAlbum = uiState.mediaViewerMessageId?.let { id ->
                    albums.firstOrNull { it.id == id }
                }
                val viewerPages = buildMediaViewerPages(
                    messages = uiState.messages,
                    albums = albums,
                    focusMessageId = focusedAlbum?.id ?: mediaViewerMessage.id,
                )
                PhotoViewerScreen(
                    pages = viewerPages,
                    initialPageIndex = focusedAlbum?.let {
                        (uiState.mediaViewerAlbumIndex ?: 0)
                            .coerceIn(0, viewerPages.lastIndex.coerceAtLeast(0))
                    } ?: findMediaViewerPageIndex(viewerPages, mediaViewerMessage.id),
                    room = uiState.room,
                    reactionsByMessage = uiState.reactionsByMessage,
                    myReactionByMessage = uiState.myReactionByMessage,
                    isPro = uiState.isPro,
                    onDismiss = { onAction(RoomAction.DismissMediaViewer) },
                    onSave = { messageId ->
                        if (uiState.isPro) {
                            onAction(RoomAction.SaveMedia(messageId))
                        } else {
                            showNeedPro = true
                        }
                    },
                    onAction = onAction,
                )
            }
            else -> {
                MediaViewerDialog(
                    message = mediaViewerMessage,
                    isPro = uiState.isPro,
                    onDismiss = { onAction(RoomAction.DismissMediaViewer) },
                    onSave = {
                        if (uiState.isPro) {
                            onAction(RoomAction.SaveMedia(mediaViewerMessage.id))
                        } else {
                            showNeedPro = true
                        }
                    },
                )
            }
        }
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
        val status = stringResource(
            when {
                detailsMessage.direction != ChatMessage.DIRECTION_OUT ->
                    R.string.room_details_status_received
                isMessageAtOrBeforeWatermark(
                    detailsMessage.id,
                    uiState.peerReadWatermarkId,
                    uiState.messages,
                ) -> R.string.room_details_status_read
                else -> R.string.room_details_status_sent
            },
        )
        VanishXAlertDialog(
            title = stringResource(R.string.room_action_details),
            body = stringResource(
                R.string.room_details_sent_at,
                formatMessageTime(detailsMessage.sentAt),
            ) + "\n" + status,
            confirmLabel = stringResource(R.string.room_safety_close),
            onConfirm = { onAction(RoomAction.DismissMessageDetails) },
            onDismiss = { onAction(RoomAction.DismissMessageDetails) },
        )
    }

    if (showNeedPro) {
        VanishXAlertDialog(
            title = stringResource(R.string.need_pro_title),
            body = stringResource(R.string.need_pro_body),
            confirmLabel = stringResource(R.string.need_pro_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Accent,
            onConfirm = {
                showNeedPro = false
                onAction(RoomAction.OpenPaywall)
            },
            onDismiss = { showNeedPro = false },
        )
    }
}

private fun RoomAction.opensAnotherRoomSurface(): Boolean = when (this) {
    RoomAction.Back,
    RoomAction.AttachClicked,
    RoomAction.OpenBlockConfirm,
    RoomAction.OpenReport,
    RoomAction.OpenPaywall,
    RoomAction.OpenInviteSheet,
    RoomAction.OpenRoomOptions,
    RoomAction.OpenMediaLibrary,
    RoomAction.OpenWallpaperSheet,
    RoomAction.OpenBentoSheet,
    RoomAction.OpenSafetySheet,
    RoomAction.OpenBurnConfirm,
    RoomAction.OpenRenameDialog,
    RoomAction.OpenRoomSearch,
    RoomAction.RequestSensitiveSend,
    is RoomAction.OpenMediaViewer,
    is RoomAction.OpenMessageActions,
    -> true
    else -> false
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
        "m4a", "aac" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "md", "markdown" -> "text/markdown"
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

@Composable
private fun VoicePermissionDialog(
    showRationale: Boolean,
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    VanishXAlertDialog(
        title = stringResource(R.string.room_voice_permission_title),
        body = stringResource(
            when {
                permanentlyDenied -> R.string.room_voice_permission_denied_body
                showRationale -> R.string.room_voice_permission_rationale
                else -> R.string.room_voice_permission_body
            },
        ),
        confirmLabel = stringResource(
            if (permanentlyDenied) {
                R.string.room_permission_open_settings
            } else {
                R.string.room_voice_permission_allow
            },
        ),
        dismissLabel = stringResource(R.string.action_back),
        tone = VanishXAlertTone.Accent,
        onConfirm = onAllow,
        onDismiss = onDismiss,
    )
}
