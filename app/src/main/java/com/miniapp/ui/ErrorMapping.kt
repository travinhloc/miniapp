package com.miniapp.ui

import android.content.Context
import com.miniapp.R
import com.miniapp.domain.exceptions.ApiException
import com.miniapp.extensions.showToast

fun Throwable.userReadableMessage(context: Context): String {
    return when (this) {
        is ApiException -> error?.message
        else -> message
    } ?: context.getString(R.string.error_generic)
}

fun Throwable.showToast(context: Context) =
    context.showToast(userReadableMessage(context))
