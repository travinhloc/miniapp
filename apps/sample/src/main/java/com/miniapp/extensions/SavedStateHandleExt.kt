package com.miniapp.extensions

import androidx.lifecycle.SavedStateHandle

fun <T> SavedStateHandle.getThenRemove(key: String): T? {
    return if (contains(key)) {
        val value = get<T>(key)
        remove<T>(key)
        value
    } else null
}
