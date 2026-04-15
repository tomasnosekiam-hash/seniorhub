package com.seniorhub.os

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seniorhub.os.data.AppRole
import com.seniorhub.os.data.AppRoleStore
import com.seniorhub.os.data.MvpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Obnovení FCM tokenu na dokumentu zařízení.
 *
 * Cloud Functions posílají FCM s `notification` + `data`. Když je aplikace **v popředí**,
 * systém notifikaci v liště **nezobrazí** — zobrazíme ji zde (stejný kanál jako systém v pozadí).
 * Překryv „Vzkaz od rodiny“ dál řeší realtime Firestore v [com.seniorhub.os.ui.HomeViewModel].
 */
class SeniorHubMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val auth = FirebaseAuth.getInstance()
                val roleStore = AppRoleStore(applicationContext)
                val role = roleStore.getRoleOrNull()
                when {
                    role == AppRole.Admin -> {
                        val user = auth.currentUser
                        if (user == null || user.isAnonymous) return@runCatching
                        val installId = FirebaseInstallations.getInstance().id.await()
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .collection("fcmTokens")
                            .document(installId)
                            .set(
                                mapOf(
                                    "token" to token,
                                    "platform" to "android",
                                    "updatedAt" to FieldValue.serverTimestamp(),
                                ),
                                com.google.firebase.firestore.SetOptions.merge(),
                            )
                            .await()
                    }
                    else -> {
                        val deps = (application as? SeniorHubApp)?.messagingDeps ?: return@runCatching
                        if (auth.currentUser == null) {
                            auth.signInAnonymously().await()
                        }
                        FirebaseFirestore.getInstance()
                            .collection(MvpRepository.COLLECTION)
                            .document(deps.deviceId)
                            .set(
                                mapOf(
                                    MvpRepository.KEY_FCM_REGISTRATION_TOKEN to token,
                                    "lastSeenAt" to FieldValue.serverTimestamp(),
                                ),
                                com.google.firebase.firestore.SetOptions.merge(),
                            )
                            .await()
                    }
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val t = data["type"] ?: return
        if (t != "new_message" && t != "new_message_admin_copy") return

        val title = message.notification?.title?.trim()?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.notification_new_message_title)
        val body = message.notification?.body?.trim()?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.notification_new_message_fallback)

        showForegroundAwareMessageNotification(title, body)
    }

    private fun showForegroundAwareMessageNotification(title: String, body: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FROM_MESSAGE_NOTIFICATION, true)
        }
        val pending = PendingIntent.getActivity(
            this,
            REQUEST_CODE_MESSAGE_TAP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, SeniorHubApp.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_simple)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        nm.notify(NOTIFICATION_ID_INCOMING_MESSAGE, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_INCOMING_MESSAGE = 71001
        private const val REQUEST_CODE_MESSAGE_TAP = 1
    }
}
