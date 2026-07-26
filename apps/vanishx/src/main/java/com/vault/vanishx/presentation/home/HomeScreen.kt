package com.vault.vanishx.presentation.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.miniapp.core.ui.theme.ComposeTheme
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigator: (destination: BaseDestination) -> Unit,
) = BaseScreen {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val placeholderMessage = stringResource(id = R.string.home_action_placeholder)

    viewModel.error.collectAsEffect { /* reserved for later stories */ }
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeAction.Resume)
    }

    LaunchedEffect(uiState.showPlaceholder) {
        if (uiState.showPlaceholder) {
            Toast.makeText(context, placeholderMessage, Toast.LENGTH_SHORT).show()
            viewModel.onAction(HomeAction.ClearPlaceholder)
        }
    }

    HomeScreenContent(
        anonymousId = uiState.anonymousId,
        showMailboxSmoke = uiState.showMailboxSmoke,
        isMailboxSmokeRunning = uiState.isMailboxSmokeRunning,
        mailboxSmokeResult = uiState.mailboxSmokeResult,
        mailboxSmokeError = uiState.mailboxSmokeError,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HomeScreenContent(
    anonymousId: String?,
    showMailboxSmoke: Boolean,
    isMailboxSmokeRunning: Boolean,
    mailboxSmokeResult: String?,
    mailboxSmokeError: String?,
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
        if (showMailboxSmoke) {
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            TextButton(
                onClick = { onAction(HomeAction.RunMailboxSmoke) },
                enabled = !isMailboxSmokeRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.home_rtdb_smoke))
            }
        }
        if (isMailboxSmokeRunning) {
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = stringResource(id = R.string.home_rtdb_smoke_running),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            mailboxSmokeError != null -> {
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                Text(
                    text = stringResource(id = R.string.home_rtdb_smoke_log_error),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                SelectionContainer {
                    Text(
                        text = mailboxSmokeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                    )
                }
                TextButton(onClick = { onAction(HomeAction.ClearMailboxSmokeFeedback) }) {
                    Text(text = stringResource(id = R.string.home_rtdb_smoke_clear_log))
                }
            }
            mailboxSmokeResult != null -> {
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                Text(
                    text = stringResource(id = R.string.home_rtdb_smoke_log_ok),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                SelectionContainer {
                    Text(
                        text = mailboxSmokeResult,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = { onAction(HomeAction.ClearMailboxSmokeFeedback) }) {
                    Text(text = stringResource(id = R.string.home_rtdb_smoke_clear_log))
                }
            }
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
            showMailboxSmoke = true,
            isMailboxSmokeRunning = false,
            mailboxSmokeResult = null,
            mailboxSmokeError = "FirebaseDatabaseException: Permission denied",
            onAction = {},
        )
    }
}
