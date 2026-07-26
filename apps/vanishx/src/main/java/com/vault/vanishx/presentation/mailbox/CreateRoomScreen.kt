package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
fun CreateRoomScreen(
    viewModel: CreateRoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    CreateRoomContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun CreateRoomContent(
    uiState: CreateRoomUiState,
    onAction: (CreateRoomAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.create_title),
            style = MaterialTheme.typography.headlineSmall,
            color = VanishXColors.OnSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = VanishXColors.Muted,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = { onAction(CreateRoomAction.NicknameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isCreating,
            singleLine = true,
            label = { Text(text = stringResource(R.string.create_nickname_label)) },
            placeholder = { Text(text = stringResource(R.string.create_nickname_hint)) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { onAction(CreateRoomAction.TitleChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isCreating,
            singleLine = true,
            label = { Text(text = stringResource(R.string.create_room_name_label)) },
            placeholder = { Text(text = stringResource(R.string.create_room_name_hint)) },
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.create_ttl_label),
            style = MaterialTheme.typography.labelLarge,
            color = VanishXColors.OnSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                    enabled = !uiState.isCreating,
                    label = { Text(text = ttlLabel(ttl)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VanishXColors.Primary.copy(alpha = 0.18f),
                        selectedLabelColor = VanishXColors.Primary,
                        containerColor = VanishXColors.Surface,
                        labelColor = VanishXColors.Muted,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.inviteNote,
            onValueChange = { onAction(CreateRoomAction.InviteNoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isCreating,
            minLines = 2,
            label = { Text(text = stringResource(R.string.create_invite_note_label)) },
            placeholder = { Text(text = stringResource(R.string.create_invite_note_hint)) },
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onAction(CreateRoomAction.Create) },
            enabled = !uiState.isCreating,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
        ) {
            Text(
                text = if (uiState.isCreating) {
                    stringResource(R.string.create_creating)
                } else {
                    stringResource(R.string.create_action)
                },
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = VanishXColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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
