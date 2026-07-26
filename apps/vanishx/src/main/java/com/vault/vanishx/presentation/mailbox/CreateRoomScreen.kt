package com.vault.vanishx.presentation.mailbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.presentation.extensions.collectAsEffect

@Composable
fun CreateRoomScreen(
    viewModel: CreateRoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    CreateRoomContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onCopy = { uri ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("VanishX invite", uri))
            Toast.makeText(context, context.getString(R.string.create_copied), Toast.LENGTH_SHORT).show()
        },
        onShare = { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, uri)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.create_share)))
        },
    )
}

@Composable
private fun CreateRoomContent(
    uiState: CreateRoomUiState,
    onAction: (CreateRoomAction) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(all = dimensions.spacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.create_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(dimensions.spacingMedium),
        )
        Text(
            text = stringResource(R.string.create_ttl_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(dimensions.spacingSmall),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoomTtlOption.entries.forEach { ttl ->
                FilterChip(
                    selected = uiState.selectedTtl == ttl,
                    onClick = { onAction(CreateRoomAction.SelectTtl(ttl)) },
                    enabled = uiState.inviteUri == null,
                    label = { Text(text = ttlLabel(ttl)) },
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(dimensions.spacingMedium),
        )
        if (uiState.inviteUri == null) {
            Button(
                onClick = { onAction(CreateRoomAction.Create) },
                enabled = !uiState.isCreating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (uiState.isCreating) {
                        stringResource(R.string.create_creating)
                    } else {
                        stringResource(R.string.create_action)
                    },
                )
            }
        } else {
            Text(
                text = stringResource(R.string.create_share_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(dimensions.spacingSmall),
            )
            uiState.qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.create_qr_cd),
                    modifier = Modifier.size(220.dp),
                )
            }
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(dimensions.spacingSmall),
            )
            SelectionContainer {
                Text(
                    text = uiState.inviteUri.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(dimensions.spacingSmall),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { uiState.inviteUri?.let(onCopy) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.create_copy))
                }
                Button(
                    onClick = { uiState.inviteUri?.let(onShare) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.create_share))
                }
            }
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(dimensions.spacingSmall),
            )
            Button(
                onClick = { onAction(CreateRoomAction.OpenRoom) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.create_enter_room))
            }
            Text(
                text = stringResource(R.string.create_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions.spacingSmall),
            )
        }
        uiState.errorMessage?.let { error ->
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(dimensions.spacingSmall),
            )
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(dimensions.spacingMedium),
        )
        TextButton(onClick = { onAction(CreateRoomAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun ttlLabel(ttl: RoomTtlOption): String = when (ttl) {
    RoomTtlOption.ONE_HOUR -> stringResource(R.string.ttl_1h)
    RoomTtlOption.SIX_HOURS -> stringResource(R.string.ttl_6h)
    RoomTtlOption.ONE_DAY -> stringResource(R.string.ttl_24h)
    RoomTtlOption.SEVEN_DAYS -> stringResource(R.string.ttl_7d)
}
