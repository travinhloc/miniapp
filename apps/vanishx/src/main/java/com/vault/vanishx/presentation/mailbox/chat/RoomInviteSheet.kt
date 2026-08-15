package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.qr.QrBitmapEncoder
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomInviteSheet(
    inviteUri: String,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val qrEncoder = remember { QrBitmapEncoder() }
    val qrBitmap = remember(inviteUri) { qrEncoder.encode(inviteUri) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.room_invite_waiting_pill),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.NeonAmber,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VanishXColors.NeonAmber.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            Text(
                text = stringResource(R.string.room_invite_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.room_invite_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.room_handshake_qr_cd),
                    modifier = Modifier
                        .size(RoomUiDimens.qrSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoomUiDimens.spacingSmall),
            ) {
                Button(
                    onClick = { onCopy(inviteUri) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanishXColors.Primary,
                        contentColor = VanishXColors.OnPrimary,
                    ),
                ) {
                    Text(text = stringResource(R.string.create_copy))
                }
                OutlinedButton(
                    onClick = { onShare(inviteUri) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.create_share))
                }
            }
            Spacer(modifier = Modifier.vanishxSheetInsets())
        }
    }
}
