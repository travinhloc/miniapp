package com.vault.vanishx.presentation.mailbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Warm-start FCM tap while Main is already showing Home (singleTop). */
@Singleton
class PendingOpenRoomStore @Inject constructor() {
    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    fun offer(roomId: String) {
        val id = roomId.trim().takeIf { it.isNotEmpty() } ?: return
        if (_roomId.value == id) {
            _roomId.value = null
        }
        _roomId.value = id
    }

    fun consume() {
        _roomId.value = null
    }
}
