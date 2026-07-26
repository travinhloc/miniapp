package com.vault.vanishx.presentation.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.miniapp.core.ui.theme.ComposeTheme
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigator: (destination: BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.error.collectAsEffect { /* reserved */ }
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeAction.Resume)
    }

    HomeScreenContent(
        anonymousId = uiState.anonymousId,
        showProStubToggle = uiState.showProStubToggle,
        isProStub = uiState.isProStub,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HomeScreenContent(
    anonymousId: String?,
    showProStubToggle: Boolean,
    isProStub: Boolean,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(all = dimensions.spacingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        Text(
            text = stringResource(id = R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (anonymousId != null) {
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = stringResource(id = R.string.home_anonymous_id, anonymousId),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        Button(
            onClick = { onAction(HomeAction.CreateRoom) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.home_create_room))
        }
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        OutlinedButton(
            onClick = { onAction(HomeAction.JoinRoom) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.home_join_room))
        }
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        TextButton(
            onClick = { onAction(HomeAction.OpenSecurity) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.home_security))
        }
        if (showProStubToggle) {
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            TextButton(
                onClick = { onAction(HomeAction.ToggleProStub) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        id = if (isProStub) R.string.home_pro_stub_on else R.string.home_pro_stub_off,
                    ),
                )
            }
            Text(
                text = stringResource(id = R.string.home_pro_stub_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        Text(
            text = stringResource(id = R.string.home_arrow_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    ComposeTheme {
        HomeScreenContent(
            anonymousId = "vx_AbCdEfGhIjKlMnOpQrStUv",
            showProStubToggle = true,
            isProStub = false,
            onAction = {},
        )
    }
}
