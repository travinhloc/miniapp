package com.vault.vanishx.data.push

import javax.inject.Inject
import javax.inject.Singleton

/** Currently resumed room, if any — used to suppress FCM trays (Epic 15.2). */
@Singleton
class RoomForegroundTracker @Inject constructor() {

    @Volatile
    var roomId: String? = null
        private set

    fun enter(roomId: String) {
        this.roomId = roomId
    }

    fun leave(roomId: String) {
        if (this.roomId == roomId) {
            this.roomId = null
        }
    }
}
