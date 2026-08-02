package com.vault.vanishx.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.util.formatRemainingMs

private val CardCorner = 16.dp
private const val CARD_BORDER_ALPHA = 0.06f

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
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(HistoryAction.Back) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = VanishXColors.Muted,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
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

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.rooms.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VanishXColors.Muted,
                    modifier = Modifier.padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
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
        }

        Text(
            text = stringResource(R.string.history_note),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = VanishXColors.Muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VanishXColors.Primary,
            selectedLabelColor = VanishXColors.OnPrimary,
            containerColor = VanishXColors.Surface,
            labelColor = VanishXColors.Muted,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = VanishXColors.Outline,
            selectedBorderColor = VanishXColors.Primary,
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
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VanishXColors.Primary.copy(alpha = CARD_BORDER_ALPHA),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = VanishXColors.OnSurface,
                    )
                    Text(
                        text = historyMetaLabel(room.meta),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = VanishXColors.Muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = historyBadgeLabel(room),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = when {
                        room.isLeft -> VanishXColors.Muted
                        room.isExpired -> VanishXColors.Accent
                        else -> VanishXColors.Primary
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
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
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = ButtonDefaults.ContentPadding,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = VanishXColors.Accent,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                VanishXColors.Accent.copy(alpha = 0.5f),
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.history_open_pro),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onOpen,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VanishXColors.Primary,
                                contentColor = VanishXColors.OnPrimary,
                            ),
                            contentPadding = ButtonDefaults.ContentPadding,
                        ) {
                            Text(
                                text = stringResource(R.string.home_open),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
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
