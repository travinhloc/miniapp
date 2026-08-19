package com.vault.vanishx.data.push

/**
 * Subscribe / unsubscribe FCM topics for rooms (story 3.1).
 * Fan-out send is out of band (Console / Functions); clients only subscribe.
 */
interface RoomPushTopics {
    suspend fun subscribe(roomId: String)
    suspend fun unsubscribe(roomId: String)

    /** After mute: FCM topic resubscribe is flaky unless we subscribe twice. */
    suspend fun resubscribe(roomId: String) {
        subscribe(roomId)
    }

    companion object {
        fun topicFor(roomId: String): String = "vx_room_$roomId"
    }
}
