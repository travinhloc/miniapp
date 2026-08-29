package com.vault.vanishx.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun appDetailsSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
