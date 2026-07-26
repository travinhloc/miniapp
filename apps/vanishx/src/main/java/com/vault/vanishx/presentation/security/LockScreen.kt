package com.vault.vanishx.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
import com.vault.vanishx.data.security.SecurityPinStore

@Composable
fun LockScreen(
    viewModel: LockViewModel = hiltViewModel(),
    onUnlocked: () -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }

    LockContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun LockContent(
    uiState: LockUiState,
    onAction: (LockAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.lock_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        Text(
            text = stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        OutlinedTextField(
            value = uiState.pin,
            onValueChange = { onAction(LockAction.PinChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBusy,
            label = { Text(text = stringResource(R.string.lock_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAction(LockAction.Submit) },
            ),
            singleLine = true,
        )
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        if (uiState.isBusy) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(text = stringResource(R.string.lock_wiping))
        } else {
            Button(
                onClick = { onAction(LockAction.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.pin.length >= SecurityPinStore.PIN_MIN_LENGTH,
            ) {
                Text(text = stringResource(R.string.lock_unlock))
            }
        }
    }
}
