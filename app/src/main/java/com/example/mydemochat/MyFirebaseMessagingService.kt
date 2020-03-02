package com.example.mydemochat

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notificationTitle = remoteMessage.notification?.title.toString()
        val notificationBody = remoteMessage.notification?.body.toString()

        val clickAction = remoteMessage.notification?.clickAction
        val fromUserId = remoteMessage.data["from_user_id"]
        val resultIntent = Intent(clickAction)
        resultIntent.putExtra("userId", fromUserId)
        val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setContentIntent(resultPendingIntent)


        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

}
