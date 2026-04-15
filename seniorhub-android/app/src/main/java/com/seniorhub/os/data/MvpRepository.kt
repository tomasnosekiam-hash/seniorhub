package com.seniorhub.os.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.seniorhub.os.util.normalizePhoneForDial
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.random.Random

class MvpRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val deviceId: String,
) {

    private val deviceRef get() = db.collection(COLLECTION).document(deviceId)
    private val configRef get() = deviceRef.collection(SUB_CONFIG).document(CONFIG_DOC_ID)
    private val statusRef get() = deviceRef.collection(SUB_STATUS).document(STATUS_DOC_ID)
    private val deviceAuthUid get() = auth.currentUser?.uid.orEmpty()

    suspend fun signInDevice() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    suspend fun bootstrapDevice() {
        signInDevice()
        val snapshot = deviceRef.get().await()
        deviceRef.set(
            mapOf(
                KEY_DEVICE_ID to deviceId,
                KEY_DEVICE_AUTH_UID to deviceAuthUid,
                KEY_DEVICE_LABEL to snapshot.getString(KEY_DEVICE_LABEL).orEmpty().ifBlank { "Tablet" },
                KEY_VOLUME_PERCENT to (snapshot.getLong(KEY_VOLUME_PERCENT) ?: 50L).toInt(),
                KEY_PAIRED to (snapshot.getBoolean(KEY_PAIRED) == true),
                "createdAt" to (snapshot.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
        ensureDeviceConfig()
        rotatePairingCodeIfNeeded(force = false)
    }

    /**
     * Zajistí výchozí 4místný PIN a výchozí jméno asistenta, pokud dokument `config` ještě neexistuje
     * nebo chybí PIN (synchronizace se spec — první spuštění tabletu).
     */
    private suspend fun ensureDeviceConfig() {
        val snap = configRef.get().await()
        val existingPin = snap.getString(KEY_ADMIN_PIN)?.trim().orEmpty()
        if (existingPin.length == 4 && existingPin.all { it.isDigit() }) {
            return
        }
        val pin = generateAdminPin()
        configRef.set(
            mapOf(
                KEY_ADMIN_PIN to pin,
                KEY_SIM_NUMBER to snap.getString(KEY_SIM_NUMBER).orEmpty(),
                KEY_ASSISTANT_NAME to (snap.getString(KEY_ASSISTANT_NAME)?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: DEFAULT_ASSISTANT_NAME),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    private fun generateAdminPin(): String =
        (0..9999).random().toString().padStart(4, '0')

    fun observeDeviceConfig(): Flow<Result<DeviceConfig?>> = callbackFlow {
        val registration = configRef.addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.failure(error))
                snapshot == null || !snapshot.exists() -> trySend(Result.success(null))
                else -> {
                    val pin = snapshot.getString(KEY_ADMIN_PIN)?.trim().orEmpty()
                    val sim = snapshot.getString(KEY_SIM_NUMBER)?.trim().orEmpty()
                    val assistant = snapshot.getString(KEY_ASSISTANT_NAME)?.trim().orEmpty()
                        .ifEmpty { DEFAULT_ASSISTANT_NAME }
                    val seniorFirst = snapshot.getString(KEY_SENIOR_FIRST_NAME)?.trim().orEmpty()
                    val seniorLast = snapshot.getString(KEY_SENIOR_LAST_NAME)?.trim().orEmpty()
                    val address = snapshot.getString(KEY_ADDRESS_LINE)?.trim().orEmpty()
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

    fun observeDevice(): Flow<Result<DeviceSettings?>> = callbackFlow {
        var lastDevice: com.google.firebase.firestore.DocumentSnapshot? = null
        var lastStatus: com.google.firebase.firestore.DocumentSnapshot? = null

        fun emitMerged() {
            val snap = lastDevice ?: return
            when {
                !snap.exists() -> trySend(Result.success(null))
                else -> {
                    val label = snap.getString(KEY_DEVICE_LABEL)?.trim().orEmpty()
                        .ifEmpty { "Tablet" }
                    val volume = (snap.getLong(KEY_VOLUME_PERCENT) ?: 50L).toInt()
                        .coerceIn(0, 100)
                    val alert = snap.getString(KEY_ALERT_MESSAGE)?.trim().orEmpty()
                        .ifEmpty { null }
                    val paired = snap.getBoolean(KEY_PAIRED) == true
                    val pairingCode = snap.getString(KEY_PAIRING_CODE)?.trim().orEmpty()
                        .ifEmpty { null }
                    val pairingExpiresAtLabel = snap.getTimestamp(KEY_PAIRING_EXPIRES_AT)
                        ?.toPairingLabel()
                    val batteryRaw = snap.getLong(KEY_BATTERY_PERCENT)
                    val batteryPercent = batteryRaw?.toInt()?.coerceIn(0, 100)
                    val charging = snap.getBoolean(KEY_CHARGING) == true
                    val lastHeartbeatAtLabel = snap.getTimestamp(KEY_LAST_HEARTBEAT_AT)
                        ?.toHeartbeatLabel()
                    val st = lastStatus
                    val networkLabel = if (st != null && st.exists()) {
                        st.getString(KEY_NETWORK_LABEL)?.trim().orEmpty().ifEmpty { null }
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

        val regDevice = deviceRef.addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.failure(error))
                else -> {
                    lastDevice = snapshot
                    emitMerged()
                }
            }
        }
        val regStatus = statusRef.addSnapshotListener { snapshot, error ->
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

    fun observeMessages(): Flow<Result<List<DeviceMessage>>> = callbackFlow {
        val registration = deviceRef.collection(SUB_MESSAGES)
            .orderBy(KEY_CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null -> trySend(Result.success(emptyList()))
                    else -> {
                        val list = snapshot.documents.map { doc -> deviceMessageFromDoc(doc) }
                        trySend(Result.success(list))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun markMessageRead(messageId: String) {
        signInDevice()
        deviceRef.collection(SUB_MESSAGES).document(messageId).update(
            mapOf(KEY_READ_AT to FieldValue.serverTimestamp()),
        ).await()
    }

    /**
     * Nepřečtený vzkaz od rodiny (bez `delivery` = ne odchozí SMS ani příchozí SMS zrcadlo).
     * Nejnovější dle `createdAt`.
     */
    suspend fun fetchFirstUnreadFamilyMessage(): DeviceMessage? {
        signInDevice()
        val snapshot = deviceRef.collection(SUB_MESSAGES)
            .orderBy(KEY_CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        for (doc in snapshot.documents) {
            val msg = deviceMessageFromDoc(doc)
            if (msg.body.isBlank()) continue
            if (!msg.delivery.isNullOrBlank()) continue
            if (msg.readAt != null) continue
            return msg
        }
        return null
    }

    private fun deviceMessageFromDoc(doc: DocumentSnapshot): DeviceMessage =
        DeviceMessage(
            id = doc.id,
            body = doc.getString(KEY_BODY)?.trim().orEmpty(),
            createdAt = doc.getTimestamp(KEY_CREATED_AT),
            readAt = doc.getTimestamp(KEY_READ_AT),
            senderDisplayName = doc.getString(KEY_SENDER_DISPLAY_NAME)?.trim()
                ?.takeIf { it.isNotEmpty() },
            delivery = doc.getString(KEY_DELIVERY)?.trim().orEmpty()
                .ifEmpty { null },
            outboundPhone = doc.getString(KEY_OUTBOUND_PHONE)?.trim().orEmpty()
                .ifEmpty { null },
            outboundName = doc.getString(KEY_OUTBOUND_NAME)?.trim().orEmpty()
                .ifEmpty { null },
            inboundFromPhone = doc.getString(KEY_INBOUND_FROM_PHONE)?.trim().orEmpty()
                .ifEmpty { null },
            inboundFromName = doc.getString(KEY_INBOUND_FROM_NAME)?.trim().orEmpty()
                .ifEmpty { null },
        )

    /**
     * Tablet bez klasické SMS: záznam do stejné kolekce jako vzkazy z webu — rodina ho uvidí ve webové administraci
     * (Firebase Firestore + případně FCM u správců v budoucnu). Není to klasická SMS na telefon příjemce.
     */
    suspend fun sendTabletFirestoreMessage(contact: Contact, body: String) {
        addOutboundDeviceMessage(contact, body.trim(), VAL_DELIVERY_TABLET_FIRESTORE)
    }

    /**
     * Po úspěšném odeslání klasické SMS — stejný dokument jako u [sendTabletFirestoreMessage], ale [VAL_DELIVERY_SMS_CELLULAR].
     * Sjednocuje vlákno u kontaktu (cloud + skutečná SMS).
     */
    suspend fun recordOutboundCellularSms(contact: Contact, body: String) {
        addOutboundDeviceMessage(contact, body.trim(), VAL_DELIVERY_SMS_CELLULAR)
    }

    /**
     * Příchozí klasická SMS od čísla odpovídajícího kontaktu — stejná kolekce jako odchozí vlákno.
     */
    suspend fun recordInboundCellularSms(
        rawFromAddress: String,
        body: String,
        matchedContact: Contact,
    ) {
        signInDevice()
        val text = body.trim()
        if (text.isEmpty()) {
            throw IllegalArgumentException("Zpráva je prázdná.")
        }
        val fromPhone = rawFromAddress.trim()
        if (fromPhone.isEmpty()) {
            throw IllegalArgumentException("Chybí číslo odesílatele.")
        }
        val displayName = matchedContact.name.trim().ifEmpty { fromPhone }
        val senderLabel = "$displayName (příchozí SMS)"
        deviceRef.collection(SUB_MESSAGES).add(
            mapOf(
                KEY_BODY to text,
                KEY_SENDER_UID to deviceAuthUid,
                KEY_SENDER_DISPLAY_NAME to senderLabel,
                KEY_CREATED_AT to FieldValue.serverTimestamp(),
                KEY_READ_AT to FieldValue.serverTimestamp(),
                KEY_DELIVERY to VAL_DELIVERY_SMS_INBOUND,
                KEY_INBOUND_FROM_PHONE to fromPhone,
                KEY_INBOUND_FROM_NAME to matchedContact.name.trim(),
            ),
        ).await()
    }

    suspend fun findContactForIncomingPhone(rawFromAddress: String): Contact? {
        signInDevice()
        val normIncoming = normalizePhoneForDial(rawFromAddress.trim()) ?: return null
        val snap = deviceRef.collection(SUB_CONTACTS).get().await()
        for (doc in snap.documents) {
            val name = doc.getString(KEY_NAME)?.trim().orEmpty()
            val phone = doc.getString(KEY_PHONE)?.trim().orEmpty()
            if (name.isEmpty() && phone.isEmpty()) continue
            val normContact = normalizePhoneForDial(phone) ?: continue
            if (normContact == normIncoming) {
                return Contact(
                    id = doc.id,
                    name = name,
                    phone = phone,
                    isEmergency = doc.getBoolean(KEY_IS_EMERGENCY) == true,
                    sortOrder = doc.getLong(KEY_SORT_ORDER) ?: 0L,
                )
            }
        }
        return null
    }

    private suspend fun addOutboundDeviceMessage(contact: Contact, body: String, delivery: String) {
        signInDevice()
        if (body.isEmpty()) {
            throw IllegalArgumentException("Zpráva je prázdná.")
        }
        val phone = contact.phone.trim()
        if (phone.isEmpty()) {
            throw IllegalArgumentException("Chybí telefonní číslo kontaktu.")
        }
        val snap = deviceRef.get().await()
        val label = snap.getString(KEY_DEVICE_LABEL)?.trim().orEmpty().ifBlank { "Tablet" }
        val senderLabel = "$label (tablet)"
        deviceRef.collection(SUB_MESSAGES).add(
            mapOf(
                KEY_BODY to body,
                KEY_SENDER_UID to deviceAuthUid,
                KEY_SENDER_DISPLAY_NAME to senderLabel,
                KEY_CREATED_AT to FieldValue.serverTimestamp(),
                // Odchozí z tabletu — nepovažovat za „nepřečtený vzkaz“ přes celou obrazovku.
                KEY_READ_AT to FieldValue.serverTimestamp(),
                KEY_DELIVERY to delivery,
                KEY_OUTBOUND_PHONE to phone,
                KEY_OUTBOUND_NAME to contact.name.trim(),
            ),
        ).await()
    }

    suspend fun registerFcmToken(token: String) {
        signInDevice()
        deviceRef.set(
            mapOf(
                KEY_FCM_REGISTRATION_TOKEN to token,
                "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    /**
     * Pravidelný „životní“ signál pro správce (baterie, nabíjení, čas posledního kontaktu).
     */
    suspend fun postDeviceHeartbeat(
        batteryPercent: Int?,
        charging: Boolean,
        networkType: String? = null,
        networkLabel: String? = null,
    ) {
        signInDevice()
        val payload = mutableMapOf<String, Any>(
            KEY_LAST_HEARTBEAT_AT to FieldValue.serverTimestamp(),
            "lastSeenAt" to FieldValue.serverTimestamp(),
            KEY_CHARGING to charging,
        )
        if (batteryPercent != null) {
            payload[KEY_BATTERY_PERCENT] = batteryPercent
        }
        deviceRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()

        val statusPayload = mutableMapOf<String, Any>(
            KEY_STATUS_UPDATED_AT to FieldValue.serverTimestamp(),
            KEY_LAST_HEARTBEAT_AT to FieldValue.serverTimestamp(),
            KEY_CHARGING to charging,
        )
        if (batteryPercent != null) {
            statusPayload[KEY_BATTERY_PERCENT] = batteryPercent
        }
        if (!networkType.isNullOrBlank()) {
            statusPayload[KEY_NETWORK_TYPE] = networkType.trim()
        }
        if (!networkLabel.isNullOrBlank()) {
            statusPayload[KEY_NETWORK_LABEL] = networkLabel.trim()
        }
        statusRef.set(statusPayload, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun observeContacts(): Flow<Result<List<Contact>>> = callbackFlow {
        val registration = deviceRef.collection(SUB_CONTACTS)
            .orderBy(KEY_SORT_ORDER, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null -> trySend(Result.success(emptyList()))
                    else -> {
                        val list = snapshot.documents.mapNotNull { doc ->
                            val name = doc.getString(KEY_NAME)?.trim().orEmpty()
                            val phone = doc.getString(KEY_PHONE)?.trim().orEmpty()
                            if (name.isEmpty() && phone.isEmpty()) return@mapNotNull null
                            Contact(
                                id = doc.id,
                                name = name,
                                phone = phone,
                                isEmergency = doc.getBoolean(KEY_IS_EMERGENCY) == true,
                                sortOrder = doc.getLong(KEY_SORT_ORDER) ?: 0L,
                            )
                        }
                        trySend(Result.success(list))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun dismissAlert() {
        deviceRef.update(
            mapOf(
                KEY_ALERT_MESSAGE to FieldValue.delete(),
                "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Zápis incidentu z modulu Matěj (nouzové vytočení) — spouští FCM pro správce přes Cloud Function. */
    suspend fun recordMatejEmergencyIncident(
        dialedPhone: String,
        dialedContactLabel: String?,
    ) {
        signInDevice()
        val phone = dialedPhone.trim().take(40)
        if (phone.isEmpty()) return
        val payload = mutableMapOf<String, Any>(
            KEY_INCIDENT_SOURCE to VAL_INCIDENT_SOURCE_MATEJ,
            KEY_INCIDENT_CREATED_AT to FieldValue.serverTimestamp(),
            KEY_INCIDENT_DIALED_PHONE to phone,
        )
        val label = dialedContactLabel?.trim()?.take(120).orEmpty()
        if (label.isNotEmpty()) {
            payload[KEY_INCIDENT_DIALED_LABEL] = label
        }
        deviceRef.collection(SUB_INCIDENTS).add(payload).await()
    }

    suspend fun rotatePairingCodeIfNeeded(force: Boolean): PairingInfo {
        signInDevice()
        val snapshot = deviceRef.get().await()
        val currentCode = snapshot.getString(KEY_PAIRING_CODE)?.trim().orEmpty()
        val currentExpires = snapshot.getTimestamp(KEY_PAIRING_EXPIRES_AT)
        val stillValid = !force &&
            currentCode.isNotBlank() &&
            currentExpires != null &&
            currentExpires.toDate().after(java.util.Date())

        if (stillValid) {
            return PairingInfo(
                code = currentCode,
                expiresAtLabel = currentExpires?.toPairingLabel(),
            )
        }

        val newCode = generatePairingCode()
        val expiresAt = Timestamp(Date.from(Instant.now().plusSeconds(PAIRING_LIFETIME_SECONDS)))
        db.batch().apply {
            set(
                deviceRef,
                mapOf(
                    KEY_PAIRING_CODE to newCode,
                    KEY_PAIRING_EXPIRES_AT to expiresAt,
                    KEY_PAIRED to (snapshot.getBoolean(KEY_PAIRED) == true),
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            set(
                db.collection(PAIRING_COLLECTION).document(newCode),
                mapOf(
                    KEY_CODE to newCode,
                    KEY_DEVICE_ID to deviceId,
                    KEY_DEVICE_AUTH_UID to deviceAuthUid,
                    KEY_EXPIRES_AT to expiresAt,
                    KEY_USED_AT to null,
                    KEY_USED_BY_UID to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.commit().await()

        return PairingInfo(
            code = newCode,
            expiresAtLabel = expiresAt.toPairingLabel(),
        )
    }

    private fun Timestamp.toPairingLabel(): String {
        val local = toDate().toInstant().atZone(ZoneId.systemDefault())
        return PAIRING_FORMATTER.format(local)
    }

    private fun Timestamp.toHeartbeatLabel(): String {
        val local = toDate().toInstant().atZone(ZoneId.systemDefault())
        return HEARTBEAT_FORMATTER.format(local)
    }

    private fun generatePairingCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }

    companion object {
        const val COLLECTION = "devices"
        /** Stejné jako web (`deviceAdmins`): vazba správce ↔ zařízení. */
        const val COLLECTION_DEVICE_ADMINS = "deviceAdmins"
        const val PAIRING_COLLECTION = "pairingClaims"
        const val SUB_CONTACTS = "contacts"
        const val SUB_MESSAGES = "messages"
        const val SUB_INCIDENTS = "incidents"
        const val SUB_CONFIG = "config"
        const val SUB_STATUS = "status"
        const val STATUS_DOC_ID = "main"
        const val KEY_BODY = "body"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_READ_AT = "readAt"
        const val KEY_SENDER_UID = "senderUid"
        const val KEY_SENDER_DISPLAY_NAME = "senderDisplayName"
        /** Náhrada SMS z tabletu bez SIM — viz Firestore rules (`tablet_firestore`). */
        const val KEY_DELIVERY = "delivery"
        const val VAL_DELIVERY_TABLET_FIRESTORE = "tablet_firestore"
        /** Odchozí klasická SMS — zrcadlo do Firestore pro jednotné vlákno. */
        const val VAL_DELIVERY_SMS_CELLULAR = "sms_cellular"
        /** Příchozí klasická SMS (číslo patří kontaktu) — stejné vlákno v UI. */
        const val VAL_DELIVERY_SMS_INBOUND = "sms_inbound"
        const val KEY_OUTBOUND_PHONE = "outbound_phone"
        const val KEY_OUTBOUND_NAME = "outbound_name"
        const val KEY_INBOUND_FROM_PHONE = "inbound_from_phone"
        const val KEY_INBOUND_FROM_NAME = "inbound_from_name"
        const val KEY_FCM_REGISTRATION_TOKEN = "fcmRegistrationToken"
        const val CONFIG_DOC_ID = "main"
        const val KEY_ADMIN_PIN = "admin_pin"
        const val KEY_SIM_NUMBER = "sim_number"
        const val KEY_ASSISTANT_NAME = "assistant_name"
        const val KEY_SENIOR_FIRST_NAME = "senior_first_name"
        const val KEY_SENIOR_LAST_NAME = "senior_last_name"
        const val KEY_ADDRESS_LINE = "address_line"
        const val DEFAULT_ASSISTANT_NAME = "Matěj"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_DEVICE_AUTH_UID = "deviceAuthUid"
        const val KEY_DEVICE_LABEL = "deviceLabel"
        const val KEY_VOLUME_PERCENT = "volumePercent"
        const val KEY_ALERT_MESSAGE = "alertMessage"
        const val KEY_PAIRED = "paired"
        const val KEY_PAIRING_CODE = "pairingCode"
        const val KEY_PAIRING_EXPIRES_AT = "pairingExpiresAt"
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_IS_EMERGENCY = "is_emergency"
        const val KEY_SORT_ORDER = "sortOrder"

        const val KEY_INCIDENT_SOURCE = "source"
        const val VAL_INCIDENT_SOURCE_MATEJ = "matej_emergency"
        const val KEY_INCIDENT_CREATED_AT = "createdAt"
        const val KEY_INCIDENT_DIALED_PHONE = "dialedPhone"
        const val KEY_INCIDENT_DIALED_LABEL = "dialedContactLabel"
        const val KEY_CODE = "code"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_USED_AT = "usedAt"
        const val KEY_USED_BY_UID = "usedByUid"

        private const val PAIRING_LIFETIME_SECONDS = 10 * 60L
        private val PAIRING_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private val HEARTBEAT_FORMATTER = DateTimeFormatter.ofPattern("d.M. HH:mm")

        const val KEY_BATTERY_PERCENT = "batteryPercent"
        const val KEY_CHARGING = "charging"
        const val KEY_LAST_HEARTBEAT_AT = "lastHeartbeatAt"
        const val KEY_STATUS_UPDATED_AT = "lastUpdatedAt"
        const val KEY_NETWORK_TYPE = "networkType"
        const val KEY_NETWORK_LABEL = "networkLabel"
    }
}
