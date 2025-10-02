package com.ivelosi.dnc.ui.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ivelosi.dnc.R

class CallNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID_CALL = "call_channel"
        const val CHANNEL_ID_INCOMING_CALL = "incoming_call_channel"
        const val CHANNEL_ID_CHAT = "chat_channel"

        const val NOTIFICATION_ID_CALL = 1001
        const val NOTIFICATION_ID_INCOMING_CALL = 1002
        const val NOTIFICATION_ID_CHAT_BASE = 2000

        const val ACTION_ANSWER_CALL = "com.ivelosi.dnc.ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.ivelosi.dnc.DECLINE_CALL"
        const val ACTION_END_CALL = "com.ivelosi.dnc.END_CALL"

        const val EXTRA_CALLER_NAME = "caller_name"
        const val EXTRA_CALLER_NID = "caller_nid"
        const val EXTRA_CONTACT_NID = "contact_nid"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for active calls
            val callChannel = NotificationChannel(
                CHANNEL_ID_CALL,
                "Active Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notification for active calls"
                setSound(null, null)
            }

            // Channel for incoming calls
            val incomingCallChannel = NotificationChannel(
                CHANNEL_ID_INCOMING_CALL,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notification for incoming calls"
                setSound(null, null)
            }

            // Channel for chat messages
            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(incomingCallChannel)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }

    fun showIncomingCallNotification(
        callerName: String,
        callerNid: Long,
        activityClass: Class<*>
    ): Notification {
        val answerIntent = Intent(context, activityClass).apply {
            action = ACTION_ANSWER_CALL
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_NID, callerNid)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val answerPendingIntent = PendingIntent.getActivity(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, activityClass).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALLER_NID, callerNid)
        }

        val declinePendingIntent = PendingIntent.getActivity(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INCOMING_CALL)
            .setSmallIcon(R.drawable.call)
            .setContentTitle("Incoming Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(answerPendingIntent, true)
            .addAction(
                R.drawable.call,
                "Answer",
                answerPendingIntent
            )
            .addAction(
                R.drawable.call_phone_24px,
                "Decline",
                declinePendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_INCOMING_CALL, notification)
        return notification
    }

    fun showActiveCallNotification(
        contactName: String,
        activityClass: Class<*>
    ): Notification {
        val contentIntent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(context, activityClass).apply {
            action = ACTION_END_CALL
        }

        val endCallPendingIntent = PendingIntent.getActivity(
            context,
            3,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CALL)
            .setSmallIcon(R.drawable.call)
            .setContentTitle("Ongoing Call")
            .setContentText("Call with $contactName")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.call_phone_24px,
                "End Call",
                endCallPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_CALL, notification)
        return notification
    }

    fun showChatMessageNotification(
        contactName: String,
        messageText: String,
        contactNid: Long,
        activityClass: Class<*>,
        messageCount: Int = 1
    ) {
        val intent = Intent(context, activityClass).apply {
            putExtra(EXTRA_CONTACT_NID, contactNid)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            contactNid.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.call) // Replace with your chat icon
            .setContentTitle(contactName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add BigTextStyle for longer messages
        if (messageText.length > 40) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText)
            )
        }

        // Show message count if multiple messages
        if (messageCount > 1) {
            notificationBuilder.setNumber(messageCount)
        }

        notificationManager.notify(
            NOTIFICATION_ID_CHAT_BASE + contactNid.toInt(),
            notificationBuilder.build()
        )
    }

    fun cancelChatNotification(contactNid: Long) {
        notificationManager.cancel(NOTIFICATION_ID_CHAT_BASE + contactNid.toInt())
    }

    fun cancelIncomingCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID_INCOMING_CALL)
    }

    fun cancelActiveCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID_CALL)
    }

    fun cancelAllCallNotifications() {
        cancelIncomingCallNotification()
        cancelActiveCallNotification()
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}