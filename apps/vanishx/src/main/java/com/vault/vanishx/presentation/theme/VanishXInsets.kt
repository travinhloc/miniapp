package com.vault.vanishx.presentation.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Status bar, navigation bar, IME, and display cutout.
 * Uses [asPaddingValues] so insets are **not** consumed — overlays can still read them.
 */
@Composable
fun Modifier.vanishxScreenInsets(): Modifier =
    padding(WindowInsets.safeDrawing.asPaddingValues())

/**
 * Fixed bottom padding for ModalBottomSheet content.
 * WindowInsets inside a sheet Dialog are often 0 on 3-button nav — do not rely on them.
 */
fun Modifier.vanishxSheetInsets(): Modifier =
    padding(bottom = SheetBottomPadding)

/** Standard 3-button nav (~48.dp) plus a little gap above the bar. */
private val SheetBottomPadding = 56.dp
