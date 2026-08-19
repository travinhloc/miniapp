package com.vault.vanishx.data.push

import com.google.firebase.messaging.FirebaseMessaging
import com.miniapp.core.common.DispatchersProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRoomPushTopics @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val dispatchersProvider: DispatchersProvider,
) : RoomPushTopics {

    override suspend fun subscribe(roomId: String) = withContext(dispatchersProvider.io) {
        subscribeOnce(RoomPushTopics.topicFor(roomId))
    }

    override suspend fun resubscribe(roomId: String) {
        withContext(dispatchersProvider.io + NonCancellable) {
            val topic = RoomPushTopics.topicFor(roomId)
            subscribeOnce(topic)
            delay(SUBSCRIBE_RETRY_DELAY_MS)
            subscribeOnce(topic)
        }
    }

    private suspend fun subscribeOnce(topic: String) {
        runCatching {
            messaging.subscribeToTopic(topic).await()
            Timber.i("Subscribed FCM topic %s", topic)
        }.onFailure { e ->
            Timber.w(e, "FCM subscribe failed for %s", topic)
        }
    }

    override suspend fun unsubscribe(roomId: String) = withContext(dispatchersProvider.io) {
        val topic = RoomPushTopics.topicFor(roomId)
        runCatching {
            messaging.unsubscribeFromTopic(topic).await()
            Timber.i("Unsubscribed FCM topic %s", topic)
        }.onFailure { e ->
            Timber.w(e, "FCM unsubscribe failed for %s", topic)
        }
        Unit
    }

    private companion object {
        const val SUBSCRIBE_RETRY_DELAY_MS = 1_500L
    }
}
