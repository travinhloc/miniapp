package com.vault.vanishx.presentation.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(uiState.showPlaceholder) {
        if (uiState.showPlaceholder) {
            Toast.makeText(context, placeholderMessage, Toast.LENGTH_SHORT).show()
            viewModel.onAction(HomeAction.ClearPlaceholder)
        }
    }

    HomeScreenContent(
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HomeScreenContent(
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
        HomeScreenContent(onAction = {})
    }
}
