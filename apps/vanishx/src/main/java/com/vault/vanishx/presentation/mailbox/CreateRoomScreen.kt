package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

private val CardCorner = 16.dp
private val ButtonCorner = 8.dp
private val ChipSelectedAlpha = 0.18f

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
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VanishXColors.Primary,
        unfocusedBorderColor = VanishXColors.Outline,
        focusedTextColor = VanishXColors.OnSurface,
        unfocusedTextColor = VanishXColors.OnSurface,
        focusedLabelColor = VanishXColors.Primary,
        unfocusedLabelColor = VanishXColors.Muted,
        cursorColor = VanishXColors.Primary,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(CreateRoomAction.Back) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }
            Text(
                text = stringResource(R.string.create_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.create_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCorner),
                color = VanishXColors.Surface,
                border = BorderStroke(1.dp, VanishXColors.Outline),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.nickname,
                        onValueChange = { onAction(CreateRoomAction.NicknameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isCreating,
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.create_nickname_label)) },
                        placeholder = { Text(text = stringResource(R.string.create_nickname_hint)) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(ButtonCorner),
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
                        colors = fieldColors,
                        shape = RoundedCornerShape(ButtonCorner),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.create_ttl_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = VanishXColors.OnSurface,
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
                                    selectedContainerColor = VanishXColors.Primary.copy(
                                        alpha = ChipSelectedAlpha,
                                    ),
                                    selectedLabelColor = VanishXColors.Primary,
                                    containerColor = VanishXColors.Surface2,
                                    labelColor = VanishXColors.Muted,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedTtl == ttl,
                                    borderColor = VanishXColors.Outline,
                                    selectedBorderColor = VanishXColors.Primary,
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
                        colors = fieldColors,
                        shape = RoundedCornerShape(ButtonCorner),
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onAction(CreateRoomAction.Create) },
                        enabled = !uiState.isCreating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(ButtonCorner),
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
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.25.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            color = VanishXColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
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
