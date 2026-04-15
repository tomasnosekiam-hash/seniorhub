package com.seniorhub.os

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
            val matej = NotificationChannel(
                CHANNEL_ID_MATEJ,
                getString(R.string.notification_channel_matej_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_matej_desc)
            }
            nm.createNotificationChannel(matej)
        }
    }

    companion object {
        const val CHANNEL_ID_MESSAGES = "family_messages"
        const val CHANNEL_ID_MATEJ = "matej_assistant"
    }
}

data class MessagingDeps(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth,
    val deviceId: String,
)
