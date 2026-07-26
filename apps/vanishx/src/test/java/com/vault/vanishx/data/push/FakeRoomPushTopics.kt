package com.vault.vanishx.data.push

/**
 * Test double that records subscribe / unsubscribe calls.
 */
class FakeRoomPushTopics : RoomPushTopics {
    val subscribed = mutableListOf<String>()
    val unsubscribed = mutableListOf<String>()

    override suspend fun subscribe(roomId: String) {
        subscribed += roomId
    }

    override suspend fun unsubscribe(roomId: String) {
        unsubscribed += roomId
    }
}
