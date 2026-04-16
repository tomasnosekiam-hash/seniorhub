package com.seniorhub.os.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class JoinedDeviceRow(
    val joinId: String,
    val deviceId: String,
    val role: String,
    val deviceLabel: String,
    val paired: Boolean,
    val pairingCode: String?,
    val pairingExpiresAtLabel: String?,
    val batteryPercent: Int?,
    val charging: Boolean,
    val lastHeartbeatAtLabel: String?,
    val networkLabel: String? = null,
)

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {

    suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Uloží FCM token pro Cloud Functions (upozornění správcům u zařízení).
     * Dokument pod [COLLECTION_USERS]/`uid`/ [SUB_FCM_TOKENS] / installationId.
     */
    suspend fun registerAdminFcmToken(token: String) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        val installId = FirebaseInstallations.getInstance().id.await()
        db.collection(COLLECTION_USERS).document(user.uid)
            .collection(SUB_FCM_TOKENS).document(installId)
            .set(
                mapOf(
                    KEY_FCM_TOKEN to token,
                    KEY_PLATFORM to "android",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
    }

    /**
     * @param uid Firebase Auth uid správce; při `null` prázdný seznam (nepřihlášeno).
     */
    fun observeJoinedDevices(uid: String?): Flow<Result<List<JoinedDeviceRow>>> = callbackFlow {
        if (uid.isNullOrBlank()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val registration = db.collection(MvpRepository.COLLECTION_DEVICE_ADMINS)
            .whereEqualTo(KEY_UID, uid)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null -> trySend(Result.success(emptyList()))
                    else -> {
                        val joinDocs = snapshot.documents
                        if (joinDocs.isEmpty()) {
                            trySend(Result.success(emptyList()))
                            return@addSnapshotListener
                        }
                        scope.launch {
                            try {
                                val rows = coroutineScope {
                                    joinDocs.map { joinDoc ->
                                        async(Dispatchers.IO) {
                                            val deviceId = joinDoc.getString(MvpRepository.KEY_DEVICE_ID)?.trim().orEmpty()
                                            if (deviceId.isEmpty()) return@async null
                                            val role = joinDoc.getString(KEY_ROLE)?.trim().orEmpty()
                                                .ifEmpty { "admin" }
                                            val deviceSnap = db.collection(MvpRepository.COLLECTION)
                                                .document(deviceId).get().await()
                                            val d = deviceSnap.data ?: emptyMap()
                                            val label = (d[MvpRepository.KEY_DEVICE_LABEL] as? String)?.trim()
                                                .orEmpty().ifEmpty { deviceId }
                                            val paired = d[MvpRepository.KEY_PAIRED] as? Boolean == true
                                            val pairingCode = (d[MvpRepository.KEY_PAIRING_CODE] as? String)?.trim()
                                                ?.takeIf { it.isNotEmpty() }
                                            val pairingExpires = (d[MvpRepository.KEY_PAIRING_EXPIRES_AT] as? Timestamp)
                                                ?.toPairingLabel()
                                            val batteryRaw = d[MvpRepository.KEY_BATTERY_PERCENT]
                                            val batteryPercent = when (batteryRaw) {
                                                is Long -> batteryRaw.toInt().coerceIn(0, 100)
                                                is Int -> batteryRaw.coerceIn(0, 100)
                                                else -> null
                                            }
                                            val charging = d[MvpRepository.KEY_CHARGING] as? Boolean == true
                                            val hb = (d[MvpRepository.KEY_LAST_HEARTBEAT_AT] as? Timestamp)
                                                ?.toHeartbeatLabel()
                                            val statusSnap = db.collection(MvpRepository.COLLECTION)
                                                .document(deviceId)
                                                .collection(MvpRepository.SUB_STATUS)
                                                .document(MvpRepository.STATUS_DOC_ID)
                                                .get().await()
                                            val networkLabel = statusSnap.getString(MvpRepository.KEY_NETWORK_LABEL)
                                                ?.trim()?.takeIf { it.isNotEmpty() }
                                            JoinedDeviceRow(
                                                joinId = joinDoc.id,
                                                deviceId = deviceId,
                                                role = role,
                                                deviceLabel = label,
                                                paired = paired,
                                                pairingCode = pairingCode,
                                                pairingExpiresAtLabel = pairingExpires,
                                                batteryPercent = batteryPercent,
                                                charging = charging,
                                                lastHeartbeatAtLabel = hb,
                                                networkLabel = networkLabel,
                                            )
                                        }
                                    }.awaitAll().filterNotNull()
                                }
                                trySend(Result.success(rows))
                            } catch (e: Exception) {
                                trySend(Result.failure(e))
                            }
                        }
                    }
                }
            }
        awaitClose {
            registration.remove()
            scope.cancel()
        }
    }

    suspend fun pairWithCode(rawCode: String) {
        val user = auth.currentUser ?: error("Nejsi přihlášen.")
        val code = rawCode.trim().uppercase()
        if (code.length < 4) {
            error("Zadej platný párovací kód z tabletu.")
        }
        val claimRef = db.collection(MvpRepository.PAIRING_COLLECTION).document(code)
        val claimSnap = claimRef.get().await()
        if (!claimSnap.exists()) {
            error("Kód neexistuje nebo už expiroval.")
        }
        val claim = claimSnap.data ?: error("Neplatná data kódu.")
        val deviceId = (claim[MvpRepository.KEY_DEVICE_ID] as? String)?.trim().orEmpty()
        if (deviceId.isEmpty()) {
            error("Párovací kód neobsahuje ID zařízení.")
        }
        if (claim[MvpRepository.KEY_USED_AT] != null) {
            error("Kód už byl použit. Na tabletu obnov kód.")
        }
        val joinId = "${deviceId}_${user.uid}"
        val joinRef = db.collection(MvpRepository.COLLECTION_DEVICE_ADMINS).document(joinId)
        val deviceRef = db.collection(MvpRepository.COLLECTION).document(deviceId)
        db.batch().apply {
            update(
                claimRef,
                mapOf(
                    MvpRepository.KEY_USED_AT to FieldValue.serverTimestamp(),
                    MvpRepository.KEY_USED_BY_UID to user.uid,
                ),
            )
            set(
                joinRef,
                mapOf(
                    MvpRepository.KEY_DEVICE_ID to deviceId,
                    KEY_UID to user.uid,
                    KEY_ROLE to "admin",
                    KEY_CLAIM_CODE to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            set(
                deviceRef,
                mapOf(
                    MvpRepository.KEY_PAIRED to true,
                    "pairedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }.commit().await()
    }

    fun observeDevice(deviceId: String): Flow<Result<DeviceSettings?>> = callbackFlow {
        val devRef = db.collection(MvpRepository.COLLECTION).document(deviceId)
        val stRef = devRef.collection(MvpRepository.SUB_STATUS).document(MvpRepository.STATUS_DOC_ID)
        var lastDevice: com.google.firebase.firestore.DocumentSnapshot? = null
        var lastStatus: com.google.firebase.firestore.DocumentSnapshot? = null

        fun emitMerged() {
            val snapshot = lastDevice ?: return
            when {
                !snapshot.exists() -> trySend(Result.success(null))
                else -> {
                    val label = snapshot.getString(MvpRepository.KEY_DEVICE_LABEL)?.trim().orEmpty()
                        .ifEmpty { "Tablet" }
                    val volume = (snapshot.getLong(MvpRepository.KEY_VOLUME_PERCENT) ?: 50L).toInt()
                        .coerceIn(0, 100)
                    val alert = snapshot.getString(MvpRepository.KEY_ALERT_MESSAGE)?.trim().orEmpty()
                        .ifEmpty { null }
                    val paired = snapshot.getBoolean(MvpRepository.KEY_PAIRED) == true
                    val pairingCode = snapshot.getString(MvpRepository.KEY_PAIRING_CODE)?.trim().orEmpty()
                        .ifEmpty { null }
                    val pairingExpiresAtLabel = snapshot.getTimestamp(MvpRepository.KEY_PAIRING_EXPIRES_AT)
                        ?.toPairingLabel()
                    val batteryRaw = snapshot.getLong(MvpRepository.KEY_BATTERY_PERCENT)
                    val batteryPercent = batteryRaw?.toInt()?.coerceIn(0, 100)
                    val charging = snapshot.getBoolean(MvpRepository.KEY_CHARGING) == true
                    val lastHeartbeatAtLabel = snapshot.getTimestamp(MvpRepository.KEY_LAST_HEARTBEAT_AT)
                        ?.toHeartbeatLabel()
                    val st = lastStatus
                    val networkLabel = if (st != null && st.exists()) {
                        st.getString(MvpRepository.KEY_NETWORK_LABEL)?.trim().orEmpty().ifEmpty { null }
                    } else {
                        null
                    }
                    trySend(
                        Result.success(
                            DeviceSettings(
                                deviceId = deviceId,
                                deviceLabel = label,
                                volumePercent = volume,
                                alertMessage = alert,
                                paired = paired,
                                pairingCode = pairingCode,
                                pairingExpiresAtLabel = pairingExpiresAtLabel,
                                batteryPercent = batteryPercent,
                                charging = charging,
                                lastHeartbeatAtLabel = lastHeartbeatAtLabel,
                                networkLabel = networkLabel,
                            ),
                        ),
                    )
                }
            }
        }

        val regDevice = devRef.addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.failure(error))
                else -> {
                    lastDevice = snapshot
                    emitMerged()
                }
            }
        }
        val regStatus = stRef.addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.failure(error))
                else -> {
                    lastStatus = snapshot
                    emitMerged()
                }
            }
        }
        awaitClose {
            regDevice.remove()
            regStatus.remove()
        }
    }

    fun observeDeviceConfig(deviceId: String): Flow<Result<DeviceConfig?>> = callbackFlow {
        val ref = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONFIG).document(MvpRepository.CONFIG_DOC_ID)
        val registration = ref.addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.failure(error))
                snapshot == null || !snapshot.exists() -> trySend(Result.success(null))
                else -> {
                    val pin = snapshot.getString(MvpRepository.KEY_ADMIN_PIN)?.trim().orEmpty()
                    val sim = snapshot.getString(MvpRepository.KEY_SIM_NUMBER)?.trim().orEmpty()
                    val assistant = snapshot.getString(MvpRepository.KEY_ASSISTANT_NAME)?.trim().orEmpty()
                    val seniorFirst = snapshot.getString(MvpRepository.KEY_SENIOR_FIRST_NAME)?.trim().orEmpty()
                    val seniorLast = snapshot.getString(MvpRepository.KEY_SENIOR_LAST_NAME)?.trim().orEmpty()
                    val address = snapshot.getString(MvpRepository.KEY_ADDRESS_LINE)?.trim().orEmpty()
                    trySend(
                        Result.success(
                            DeviceConfig(
                                adminPin = pin,
                                simNumber = sim,
                                assistantName = assistant,
                                seniorFirstName = seniorFirst,
                                seniorLastName = seniorLast,
                                addressLine = address,
                            ),
                        ),
                    )
                }
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveDeviceMeta(
        deviceId: String,
        deviceLabel: String,
        volumePercent: Int,
        alertMessage: String,
    ) {
        db.collection(MvpRepository.COLLECTION).document(deviceId).set(
            mapOf(
                MvpRepository.KEY_DEVICE_LABEL to deviceLabel.trim(),
                MvpRepository.KEY_VOLUME_PERCENT to volumePercent.coerceIn(0, 100),
                MvpRepository.KEY_ALERT_MESSAGE to alertMessage.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun clearAlert(deviceId: String) {
        db.collection(MvpRepository.COLLECTION).document(deviceId).set(
            mapOf(MvpRepository.KEY_ALERT_MESSAGE to FieldValue.delete()),
            SetOptions.merge(),
        ).await()
    }

    suspend fun saveConfigBlock(
        deviceId: String,
        adminPin: String,
        simNumber: String,
        assistantName: String,
    ) {
        val pin = adminPin.filter { it.isDigit() }.take(4)
        if (pin.length != 4) error("PIN musí mít přesně 4 číslice.")
        val ref = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONFIG).document(MvpRepository.CONFIG_DOC_ID)
        ref.set(
            mapOf(
                MvpRepository.KEY_ADMIN_PIN to pin,
                MvpRepository.KEY_SIM_NUMBER to simNumber.trim(),
                MvpRepository.KEY_ASSISTANT_NAME to assistantName.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun saveSeniorProfile(
        deviceId: String,
        seniorFirstName: String,
        seniorLastName: String,
        addressLine: String,
    ) {
        val ref = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONFIG).document(MvpRepository.CONFIG_DOC_ID)
        ref.set(
            mapOf(
                MvpRepository.KEY_SENIOR_FIRST_NAME to seniorFirstName.trim(),
                MvpRepository.KEY_SENIOR_LAST_NAME to seniorLastName.trim(),
                MvpRepository.KEY_ADDRESS_LINE to addressLine.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun sendMessage(deviceId: String, body: String) {
        val user = auth.currentUser ?: error("Nejsi přihlášen.")
        val text = body.trim()
        if (text.isEmpty()) error("Text vzkazu je prázdný.")
        db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_MESSAGES)
            .add(
                mapOf(
                    MvpRepository.KEY_BODY to text,
                    MvpRepository.KEY_SENDER_UID to user.uid,
                    MvpRepository.KEY_SENDER_DISPLAY_NAME to (
                        user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                            ?: user.email?.trim()?.takeIf { it.isNotEmpty() }
                            ?: user.uid
                        ),
                    MvpRepository.KEY_CREATED_AT to FieldValue.serverTimestamp(),
                    MvpRepository.KEY_READ_AT to null,
                ),
            ).await()
    }

    fun observeContacts(deviceId: String): Flow<Result<List<Contact>>> = callbackFlow {
        val registration = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONTACTS)
            .orderBy(MvpRepository.KEY_SORT_ORDER, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null -> trySend(Result.success(emptyList()))
                    else -> {
                        val list = snapshot.documents.map { doc ->
                            Contact(
                                id = doc.id,
                                name = doc.getString(MvpRepository.KEY_NAME)?.trim().orEmpty(),
                                phone = doc.getString(MvpRepository.KEY_PHONE)?.trim().orEmpty(),
                                isEmergency = doc.getBoolean(MvpRepository.KEY_IS_EMERGENCY) == true,
                                sortOrder = doc.getLong(MvpRepository.KEY_SORT_ORDER) ?: 0L,
                            )
                        }
                        trySend(Result.success(list))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addContact(deviceId: String, name: String, phone: String) {
        val n = name.trim()
        val p = phone.trim()
        if (n.isEmpty() && p.isEmpty()) {
            error("Vyplň jméno nebo telefon.")
        }
        db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONTACTS)
            .add(
                mapOf(
                    MvpRepository.KEY_NAME to n,
                    MvpRepository.KEY_PHONE to p,
                    MvpRepository.KEY_IS_EMERGENCY to false,
                    MvpRepository.KEY_SORT_ORDER to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    suspend fun setContactEmergency(deviceId: String, contactId: String, isEmergency: Boolean) {
        db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONTACTS)
            .document(contactId)
            .update(MvpRepository.KEY_IS_EMERGENCY, isEmergency)
            .await()
    }

    suspend fun deleteContact(deviceId: String, contactId: String) {
        db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONTACTS)
            .document(contactId)
            .delete()
            .await()
    }

    /**
     * Atomicky prohodí [MvpRepository.KEY_SORT_ORDER] mezi dvěma kontakty (stejné pořadí jako v UI).
     */
    suspend fun swapContactSortOrders(deviceId: String, contactIdA: String, contactIdB: String) {
        if (contactIdA == contactIdB) return
        val col = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_CONTACTS)
        val refA = col.document(contactIdA)
        val refB = col.document(contactIdB)
        db.runTransaction { tx ->
            val sa = tx.get(refA)
            val sb = tx.get(refB)
            if (!sa.exists() || !sb.exists()) {
                throw FirebaseFirestoreException(
                    "Kontakt neexistuje.",
                    FirebaseFirestoreException.Code.NOT_FOUND,
                )
            }
            val orderA = sa.getLong(MvpRepository.KEY_SORT_ORDER) ?: 0L
            val orderB = sb.getLong(MvpRepository.KEY_SORT_ORDER) ?: 0L
            tx.update(refA, mapOf(MvpRepository.KEY_SORT_ORDER to orderB))
            tx.update(refB, mapOf(MvpRepository.KEY_SORT_ORDER to orderA))
            null
        }.await()
    }

    fun observeMessages(deviceId: String): Flow<Result<List<DeviceMessage>>> = callbackFlow {
        val registration = db.collection(MvpRepository.COLLECTION).document(deviceId)
            .collection(MvpRepository.SUB_MESSAGES)
            .orderBy(MvpRepository.KEY_CREATED_AT, Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null -> trySend(Result.success(emptyList()))
                    else -> {
                        val list = snapshot.documents.map { doc ->
                            DeviceMessage(
                                id = doc.id,
                                body = doc.getString(MvpRepository.KEY_BODY)?.trim().orEmpty(),
                                createdAt = doc.getTimestamp(MvpRepository.KEY_CREATED_AT),
                                readAt = doc.getTimestamp(MvpRepository.KEY_READ_AT),
                                senderDisplayName = doc.getString(MvpRepository.KEY_SENDER_DISPLAY_NAME)?.trim()
                                    ?.takeIf { it.isNotEmpty() },
                                delivery = doc.getString(MvpRepository.KEY_DELIVERY)?.trim().orEmpty()
                                    .ifEmpty { null },
                                outboundPhone = doc.getString(MvpRepository.KEY_OUTBOUND_PHONE)?.trim().orEmpty()
                                    .ifEmpty { null },
                                outboundName = doc.getString(MvpRepository.KEY_OUTBOUND_NAME)?.trim().orEmpty()
                                    .ifEmpty { null },
                                inboundFromPhone = doc.getString(MvpRepository.KEY_INBOUND_FROM_PHONE)?.trim().orEmpty()
                                    .ifEmpty { null },
                                inboundFromName = doc.getString(MvpRepository.KEY_INBOUND_FROM_NAME)?.trim().orEmpty()
                                    .ifEmpty { null },
                            )
                        }
                        trySend(Result.success(list))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    private fun Timestamp.toPairingLabel(): String {
        val local = toDate().toInstant().atZone(ZoneId.systemDefault())
        return PAIRING_FORMATTER.format(local)
    }

    private fun Timestamp.toHeartbeatLabel(): String {
        val local = toDate().toInstant().atZone(ZoneId.systemDefault())
        return HEARTBEAT_FORMATTER.format(local)
    }

    private companion object {
        const val KEY_UID = "uid"
        const val KEY_ROLE = "role"
        const val KEY_CLAIM_CODE = "claimCode"
        const val COLLECTION_USERS = "users"
        const val SUB_FCM_TOKENS = "fcmTokens"
        const val KEY_FCM_TOKEN = "token"
        const val KEY_PLATFORM = "platform"

        private val PAIRING_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private val HEARTBEAT_FORMATTER = DateTimeFormatter.ofPattern("d.M. HH:mm")
    }
}
