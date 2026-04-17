package com.seniorhub.os

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.os.R

class SeniorHubApp : Application() {

    @Volatile
    var messagingDeps: MessagingDeps? = null
        private set

    fun setMessagingDeps(db: FirebaseFirestore, auth: FirebaseAuth, deviceId: String) {
        messagingDeps = MessagingDeps(db = db, auth = auth, deviceId = deviceId)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                getString(R.string.notification_channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_messages_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
            val emergency = NotificationChannel(
                CHANNEL_ID_EMERGENCY_INCIDENT,
                getString(R.string.notification_channel_emergency_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_emergency_desc)
                enableVibration(true)
            }
            nm.createNotificationChannel(emergency)
        }
    }

    companion object {
        const val CHANNEL_ID_MESSAGES = "family_messages"
        /** FCM incident / nouze (správci). */
        const val CHANNEL_ID_EMERGENCY_INCIDENT = "emergency_incidents"
    }
}

data class MessagingDeps(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth,
    val deviceId: String,
)
