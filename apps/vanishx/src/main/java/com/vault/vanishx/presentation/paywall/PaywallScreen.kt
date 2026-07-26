package com.vault.vanishx.presentation.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowActions(onBack = { onAction(PaywallAction.Back) })

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineMedium,
                color = VanishXColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VanishXColors.Surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaywallBenefit(text = stringResource(R.string.paywall_benefit_history))
                PaywallBenefit(text = stringResource(R.string.paywall_benefit_recall))
                PaywallBenefit(text = stringResource(R.string.paywall_benefit_ping))
            }
        }

        if (uiState.showStubActivate) {
            Button(
                onClick = { onAction(PaywallAction.ActivateProStub) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Accent,
                    contentColor = VanishXColors.OnSurface,
                ),
            ) {
                Text(text = stringResource(R.string.paywall_continue_pro))
            }
        } else {
            OutlinedButton(
                onClick = { onAction(PaywallAction.Back) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.paywall_not_now))
            }
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

@Composable
private fun RowActions(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Text(text = stringResource(R.string.action_back))
    }
}

@Composable
private fun PaywallBenefit(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = VanishXColors.OnSurface,
    )
}
