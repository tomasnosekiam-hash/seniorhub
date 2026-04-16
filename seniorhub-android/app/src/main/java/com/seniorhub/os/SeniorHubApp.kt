package com.seniorhub.os

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.os.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SeniorHubApp : Application() {

    @Volatile
    var messagingDeps: MessagingDeps? = null
        private set

    private val matejWakeChannel = Channel<Unit>(Channel.UNLIMITED)

    /**
     * Probuzení z Porcupine (foreground služba → UI). Kanál místo SharedFlow+tryEmit:
     * `tryEmit` na SharedFlow umí zahodit událost, dokud neběží `collect` (race při startu FGS).
     * Sbírá výhradně [com.seniorhub.os.ui.HomeViewModel].
     */
    val matejWakeSignals: Flow<Unit> = matejWakeChannel.receiveAsFlow()

    fun emitMatejWake() {
        val r = matejWakeChannel.trySend(Unit)
        if (r.isFailure) {
            Log.w(TAG, "emitMatejWake: $r")
        }
    }

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
            val matejWake = NotificationChannel(
                CHANNEL_ID_MATEJ_WAKE,
                getString(R.string.notification_channel_matej_wake_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_matej_wake_desc)
                setSound(null, null)
            }
            nm.createNotificationChannel(matejWake)
        }
    }

    companion object {
        private const val TAG = "SeniorHubApp"
        const val CHANNEL_ID_MESSAGES = "family_messages"
        /** FCM incident / nouze (správci). */
        const val CHANNEL_ID_EMERGENCY_INCIDENT = "emergency_incidents"
        /** Matěj — naslouchání klíčovému slovu (foreground). */
        const val CHANNEL_ID_MATEJ_WAKE = "matej_wake_listen"
    }
}

data class MessagingDeps(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth,
    val deviceId: String,
)
