package com.vault.vanishx.presentation.paywall

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

private val CardCorner = 16.dp
private val ButtonCorner = 8.dp
private const val LIST_BORDER_ALPHA = 0.15f

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    PaywallScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun PaywallScreenContent(
    uiState: PaywallUiState,
    onAction: (PaywallAction) -> Unit,
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
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAction(PaywallAction.Back) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.paywall_hero_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 34.sp,
                    ),
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.paywall_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VanishXColors.Muted,
                )
            }

            Surface(
                shape = RoundedCornerShape(CardCorner),
                color = VanishXColors.Surface,
                border = BorderStroke(1.dp, VanishXColors.Accent.copy(alpha = LIST_BORDER_ALPHA)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    PaywallBenefitRow(text = stringResource(R.string.paywall_benefit_history))
                    HorizontalDivider(color = VanishXColors.Outline)
                    PaywallBenefitRow(text = stringResource(R.string.paywall_benefit_recall))
                    HorizontalDivider(color = VanishXColors.Outline)
                    PaywallBenefitRow(text = stringResource(R.string.paywall_benefit_ping))
                }
            }

            if (uiState.showStubActivate) {
                Button(
                    onClick = { onAction(PaywallAction.ActivateProStub) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ButtonCorner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanishXColors.Accent,
                        contentColor = VanishXColors.OnSurface,
                    ),
                ) {
                    Text(text = stringResource(R.string.paywall_continue_pro))
                }
            }

            OutlinedButton(
                onClick = { onAction(PaywallAction.RestorePurchases) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ButtonCorner),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VanishXColors.Primary),
                border = BorderStroke(1.dp, VanishXColors.Primary),
            ) {
                Text(text = stringResource(R.string.settings_restore_purchases))
            }

            TextButton(
                onClick = { onAction(PaywallAction.Back) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.paywall_not_now),
                    color = VanishXColors.Muted,
                )
            }

            uiState.infoMessage?.let { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = stringResource(R.string.paywall_stub_note),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PaywallBenefitRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = VanishXColors.Accent,
            modifier = Modifier.height(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = VanishXColors.OnSurface,
        )
    }
}
