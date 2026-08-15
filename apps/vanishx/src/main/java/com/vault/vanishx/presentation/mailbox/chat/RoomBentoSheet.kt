package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomBentoSheet(
    uiState: com.vault.vanishx.presentation.mailbox.RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
    val recallable = findRecallableMessage(
        messages = uiState.messages,
        isPro = uiState.isPro,
        isExpired = uiState.isExpired,
        isRecalling = uiState.isRecalling,
    )

    ModalBottomSheet(
        onDismissRequest = { onAction(RoomAction.DismissBentoSheet) },
        sheetState = rememberModalBottomSheetState(),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .vanishxSheetInsets()
                .padding(horizontal = RoomUiDimens.spacingMedium)
                .padding(bottom = RoomUiDimens.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.room_bento_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))

            BentoCard(
                title = stringResource(R.string.room_bento_burn),
                subtitle = stringResource(R.string.room_bento_burn_sub),
                danger = true,
                onClick = {
                    onAction(RoomAction.DismissBentoSheet)
                    onAction(RoomAction.OpenBurnConfirm)
                },
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            BentoCard(
                title = stringResource(R.string.room_bento_ttl),
                subtitle = stringResource(R.string.room_bento_ttl_sub),
                onClick = {
                    onAction(RoomAction.DismissBentoSheet)
                    onAction(RoomAction.StubChangeTtl)
                },
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            BentoCard(
                title = stringResource(R.string.room_bento_fingerprint),
                subtitle = stringResource(R.string.room_bento_fingerprint_sub),
                onClick = {
                    onAction(RoomAction.DismissBentoSheet)
                    onAction(RoomAction.OpenSafetySheet)
                },
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            BentoCard(
                title = stringResource(R.string.room_bento_block),
                subtitle = stringResource(R.string.room_block_body),
                danger = true,
                enabled = !uiState.isBlocking,
                onClick = {
                    onAction(RoomAction.DismissBentoSheet)
                    onAction(RoomAction.OpenBlockConfirm)
                },
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            BentoCard(
                title = stringResource(R.string.room_bento_report),
                subtitle = stringResource(R.string.room_report_body),
                enabled = !uiState.isReporting,
                onClick = {
                    onAction(RoomAction.DismissBentoSheet)
                    onAction(RoomAction.OpenReport)
                },
            )
            if (recallable != null) {
                Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
                BentoCard(
                    title = stringResource(R.string.room_bento_recall),
                    subtitle = stringResource(R.string.room_recall),
                    onClick = {
                        onAction(RoomAction.DismissBentoSheet)
                        onAction(RoomAction.RecallMessage(recallable.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(RoomUiDimens.bentoCardCorner),
        color = VanishXColors.Surface2,
        border = BorderStroke(
            1.dp,
            if (danger) VanishXColors.Error.copy(alpha = 0.35f) else VanishXColors.Outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (danger) VanishXColors.Error else VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = VanishXColors.Muted,
                maxLines = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomSafetySheet(
    roomKey: String,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .vanishxSheetInsets()
                .padding(horizontal = RoomUiDimens.spacingMedium)
                .padding(bottom = RoomUiDimens.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.room_safety_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.room_safety_body),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
            Text(
                text = stringResource(R.string.room_safety_fingerprint_label),
                style = MaterialTheme.typography.labelMedium,
                color = VanishXColors.Primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VanishXColors.Surface2,
                border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = roomKeyFingerprint(roomKey),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.room_safety_close))
            }
        }
    }
}
