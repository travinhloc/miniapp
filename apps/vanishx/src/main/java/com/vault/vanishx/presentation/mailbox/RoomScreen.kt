@file:Suppress("TooManyFunctions", "ComplexMethod", "ComplexCondition")

package com.vault.vanishx.presentation.mailbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
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
import com.vault.vanishx.presentation.mailbox.chat.RoomSafetySheet
import com.vault.vanishx.presentation.mailbox.chat.RoomUiDimens
import com.vault.vanishx.presentation.mailbox.chat.WaitingStage
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
fun RoomScreen(
    viewModel: RoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    )
}

@Composable
private fun RoomContent(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
) {
    val showComposer = !uiState.isLoading &&
        uiState.room?.status == MailboxRoom.STATUS_ACTIVE &&
        !uiState.isExpired
    val roomHandshakeStatus = handshakeStatus(uiState.room, uiState.isExpired)
    val isHandshakeWaiting = roomHandshakeStatus == RoomHandshakeStatus.WAITING
    val isWaitingForPeer = isHandshakeWaiting && uiState.room?.role == MailboxRoom.ROLE_CREATOR
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
            .statusBarsPadding()
            // IME ∪ nav bars — avoid stacking imePadding + navigationBarsPadding (gap above keyboard).
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
    ) {
        RoomHeader(
            uiState = uiState,
            onAction = onAction,
            isWaitingForPeer = isWaitingForPeer,
            handshakeStatus = roomHandshakeStatus,
        )

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
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)

        if (showComposer) {
            RoomComposer(
                draft = uiState.draft,
                isSending = uiState.isSending,
                locked = isHandshakeWaiting,
                onAction = onAction,
            )
        }
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
}
