package com.vault.vanishx.presentation.history

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.util.formatRemainingMs

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    HistoryScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HistoryScreenContent(
    uiState: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onAction(HistoryAction.Back) }) {
                Text(text = stringResource(R.string.action_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryFilterChip(
                label = stringResource(R.string.history_filter_open),
                selected = uiState.filter == HistoryRoomFilter.Open,
                onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.Open)) },
            )
            HistoryFilterChip(
                label = stringResource(R.string.history_filter_expired),
                selected = uiState.filter == HistoryRoomFilter.Expired,
                onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.Expired)) },
            )
            HistoryFilterChip(
                label = stringResource(R.string.history_filter_all),
                selected = uiState.filter == HistoryRoomFilter.All,
                onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.All)) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.rooms.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            uiState.rooms.forEach { room ->
                HistoryRoomRow(
                    room = room,
                    isPro = uiState.isPro,
                    onOpen = { onAction(HistoryAction.OpenRoom(room.id)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_note),
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.Muted,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VanishXColors.Primary.copy(alpha = 0.18f),
            selectedLabelColor = VanishXColors.Primary,
            containerColor = VanishXColors.Surface,
            labelColor = VanishXColors.Muted,
        ),
    )
}

@Composable
private fun HistoryRoomRow(
    room: HistoryRoomItem,
    isPro: Boolean,
    onOpen: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = VanishXColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = historyMetaLabel(room.meta),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
            }
            Text(
                text = historyBadgeLabel(room),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    room.isLeft -> VanishXColors.Muted
                    room.isExpired -> VanishXColors.Accent
                    else -> VanishXColors.Primary
                },
            )
            when {
                room.isLeft -> {
                    Text(
                        text = stringResource(R.string.history_action_none),
                        style = MaterialTheme.typography.labelLarge,
                        color = VanishXColors.Muted,
                    )
                }
                room.isExpired && !isPro -> {
                    OutlinedButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VanishXColors.Accent,
                        ),
                    ) {
                        Text(text = stringResource(R.string.history_open_pro))
                    }
                }
                else -> {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanishXColors.Primary,
                            contentColor = VanishXColors.OnPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.home_open))
                    }
                }
            }
        }
    }
}

@Composable
private fun historyMetaLabel(meta: HistoryRoomMeta): String = when (meta) {
    HistoryRoomMeta.Creator -> stringResource(R.string.history_meta_creator)
    HistoryRoomMeta.Member -> stringResource(R.string.history_meta_member)
    HistoryRoomMeta.Archived -> stringResource(R.string.history_meta_archived)
    HistoryRoomMeta.Left -> stringResource(R.string.history_meta_left)
}

@Composable
private fun historyBadgeLabel(room: HistoryRoomItem): String = when {
    room.isLeft -> stringResource(R.string.history_badge_left)
    room.isExpired -> stringResource(R.string.badge_expired)
    else -> stringResource(R.string.badge_remaining, formatRemainingMs(room.remainingMs))
}

@Preview(showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    VanishXTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(
                rooms = listOf(
                    HistoryRoomItem(
                        id = "1",
                        displayName = "Kế hoạch cuối tuần",
                        meta = HistoryRoomMeta.Creator,
                        remainingMs = 300_000,
                        isExpired = false,
                        isLeft = false,
                    ),
                    HistoryRoomItem(
                        id = "2",
                        displayName = "Phòng ···k9f",
                        meta = HistoryRoomMeta.Archived,
                        remainingMs = 0,
                        isExpired = true,
                        isLeft = false,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
