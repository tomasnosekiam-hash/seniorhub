package com.seniorhub.os.data

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
)

data class DeviceSettings(
    val deviceId: String,
    val deviceLabel: String,
    val volumePercent: Int,
    val alertMessage: String?,
    val paired: Boolean,
    val pairingCode: String?,
    val pairingExpiresAtLabel: String?,
)

/** `devices/{id}/config/main` — admin PIN, SIM, asistent (senior / kiosk). */
data class DeviceConfig(
    val adminPin: String,
    val simNumber: String,
    val assistantName: String,
)

data class PairingInfo(
    val code: String,
    val expiresAtLabel: String?,
)
