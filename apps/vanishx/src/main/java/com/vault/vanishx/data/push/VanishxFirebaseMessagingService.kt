package com.vault.vanishx.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VanishxFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: RoomNotificationHelper

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed (len=%d)", token.length)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val roomId = message.data[KEY_ROOM_ID]?.takeIf { it.isNotBlank() }
        if (roomId == null) {
            Timber.w("FCM message missing roomId: %s", message.data.keys)
            return
        }
        notificationHelper.showRoomMessageNotification(roomId)
    }

    companion object {
        const val KEY_ROOM_ID = "roomId"
    }
}
