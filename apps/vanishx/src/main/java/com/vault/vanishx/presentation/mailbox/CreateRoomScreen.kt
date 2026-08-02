package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

private val ChoiceCorner = 14.dp
private val ButtonCorner = 8.dp

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
            .statusBarsPadding(),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ModeChoiceCard(
                    selected = uiState.mode == CreateRoomMode.INSTANT,
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.create_mode_instant_title),
                    description = stringResource(R.string.create_mode_instant_desc),
                    enabled = !uiState.isCreating,
                    onClick = { onAction(CreateRoomAction.SelectMode(CreateRoomMode.INSTANT)) },
                )
                ModeChoiceCard(
                    selected = uiState.mode == CreateRoomMode.LATER,
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.create_mode_later_title),
                    description = stringResource(R.string.create_mode_later_desc),
                    enabled = !uiState.isCreating,
                    onClick = { onAction(CreateRoomAction.SelectMode(CreateRoomMode.LATER)) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.icebreaker,
                onValueChange = { onAction(CreateRoomAction.IcebreakerChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isCreating,
                singleLine = true,
                label = { Text(text = stringResource(R.string.create_icebreaker_label)) },
                placeholder = { Text(text = stringResource(R.string.create_icebreaker_hint)) },
                supportingText = { Text(text = stringResource(R.string.create_icebreaker_help)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VanishXColors.Primary,
                    unfocusedBorderColor = VanishXColors.Outline,
                    focusedTextColor = VanishXColors.OnSurface,
                    unfocusedTextColor = VanishXColors.OnSurface,
                    focusedLabelColor = VanishXColors.Primary,
                    unfocusedLabelColor = VanishXColors.Muted,
                    cursorColor = VanishXColors.Primary,
                ),
                shape = RoundedCornerShape(ButtonCorner),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = VanishXColors.Primary.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.12f)),
            ) {
                Text(
                    text = stringResource(R.string.create_defaults_note),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    color = VanishXColors.Muted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onAction(CreateRoomAction.Back) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isCreating,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { onAction(CreateRoomAction.Create) },
                    enabled = !uiState.isCreating,
                    modifier = Modifier.weight(1f),
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

@Composable
private fun ModeChoiceCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(ChoiceCorner),
        color = VanishXColors.Surface2,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) VanishXColors.Primary else VanishXColors.Outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VanishXColors.Primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.OnSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = VanishXColors.Muted,
                )
            }
        }
    }
}
