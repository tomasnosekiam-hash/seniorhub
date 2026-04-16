package com.seniorhub.os.data

import com.google.firebase.Timestamp

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val isEmergency: Boolean = false,
    /** Řazení v seznamu (`devices/.../contacts.sortOrder`). */
    val sortOrder: Long = 0L,
)

data class DeviceSettings(
    val deviceId: String,
    val deviceLabel: String,
    val volumePercent: Int,
    val alertMessage: String?,
    val paired: Boolean,
    val pairingCode: String?,
    val pairingExpiresAtLabel: String?,
    /** Naposledy zapsáno tabletkem (Firestore). */
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
    val lastHeartbeatAtLabel: String? = null,
    /** Z `devices/.../status/main` (síť). */
    val networkLabel: String? = null,
)

/** `devices/{id}/config/main` — admin PIN, SIM, volitelné `assistant_name` (legacy), profil seniora. */
data class DeviceConfig(
    val adminPin: String,
    val simNumber: String,
    val assistantName: String,
    val seniorFirstName: String,
    val seniorLastName: String,
    val addressLine: String,
)

data class PairingInfo(
    val code: String,
    val expiresAtLabel: String?,
)

/** `devices/{id}/messages/{messageId}` — vzkaz z webu na tablet. */
data class DeviceMessage(
    val id: String,
    val body: String,
    val createdAt: Timestamp?,
    val readAt: Timestamp?,
    val senderDisplayName: String?,
    /** `tablet_firestore` = cloud bez SIM; `sms_cellular` = zrcadlo klasické SMS; `sms_inbound` = příchozí SMS do vlákna; null = web / rodina. */
    val delivery: String? = null,
    val outboundPhone: String? = null,
    val outboundName: String? = null,
    /** U [delivery] `sms_inbound` — odesílatel (telefon). */
    val inboundFromPhone: String? = null,
    val inboundFromName: String? = null,
)
