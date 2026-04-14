package com.seniorhub.os

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.seniorhub.os.data.MvpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Obnovení FCM tokenu na dokumentu zařízení. Doručení vzkazu řeší i realtime Firestore na tabletu;
 * Cloud Functions posílají FCM pro probuzení na pozadí.
 */
class SeniorHubMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val deps = (application as? SeniorHubApp)?.messagingDeps ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val auth = FirebaseAuth.getInstance()
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

    override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(message)
        // UI aktualizuje snapshot `messages`; systémová notifikace z FCM payloadu (Functions).
    }
}
