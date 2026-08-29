package com.vault.vanishx.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VanishxFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var incomingPushHandler: IncomingPushHandler

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed (len=%d)", token.length)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        runBlocking {
            incomingPushHandler.handle(message.data)
        }
    }
}
