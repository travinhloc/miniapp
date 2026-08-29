package com.vault.vanishx.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.IncomingPushType
import com.vault.vanishx.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            context.getString(R.string.fcm_channel_id),
            context.getString(R.string.fcm_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.fcm_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun showRoomPushNotification(roomId: String, type: IncomingPushType) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ensureChannel()
        val openUri = Uri.parse("vanishx://open/$roomId")
        val intent = Intent(Intent.ACTION_VIEW, openUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId(roomId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val titleRes = when (type) {
            IncomingPushType.PING -> R.string.fcm_ping_title
            IncomingPushType.MESSAGE -> R.string.fcm_notification_title
        }
        val bodyRes = when (type) {
            IncomingPushType.PING -> R.string.fcm_ping_body
            IncomingPushType.MESSAGE -> R.string.fcm_notification_body
        }
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.fcm_channel_id),
        )
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifications.notify(notificationId(roomId, type), notification)
    }

    companion object {
        fun notificationId(roomId: String, type: IncomingPushType): Int =
            "${roomId}_${type.name.lowercase()}".hashCode()
    }
}
