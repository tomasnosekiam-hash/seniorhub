package com.seniorhub.os.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast

enum class SimCardState {
    NoTelephony,
    Absent,
    PinRequired,
    PukRequired,
    NetworkLocked,
    NotReady,
    Ready,
    Unknown,
}

data class SimCardStatus(
    val state: SimCardState,
) {
    val needsUnlock: Boolean
        get() = state == SimCardState.PinRequired ||
            state == SimCardState.PukRequired ||
            state == SimCardState.NetworkLocked

    val blocksCellular: Boolean
        get() = state != SimCardState.Ready && state != SimCardState.NoTelephony
}

object SimCardStatusReader {
    fun read(context: Context): SimCardStatus {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return SimCardStatus(SimCardState.NoTelephony)
        }
        val tm = context.getSystemService(TelephonyManager::class.java)
            ?: return SimCardStatus(SimCardState.Unknown)
        val rawState = runCatching { tm.simState }.getOrDefault(TelephonyManager.SIM_STATE_UNKNOWN)
        return SimCardStatus(mapSimState(rawState))
    }

    fun openUnlockFlow(context: Context): Boolean {
        val candidates = listOf(
            Intent("android.settings.SIM_CARD_SETTINGS"),
            Intent("android.settings.SIM_SETTINGS"),
            intentForComponent(
                "com.android.settings",
                "com.android.settings.Settings\$SimSettingsActivity",
            ),
            intentForComponent(
                "com.android.settings",
                "com.android.settings.network.telephony.MobileNetworkSettings",
            ),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            if (intent == null) continue
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            }
        }
        Toast.makeText(
            context,
            "Otevřete Nastavení → Síť a internet → SIM karta a zadejte PIN.",
            Toast.LENGTH_LONG,
        ).show()
        return false
    }

    fun bannerMessage(status: SimCardStatus): String = when (status.state) {
        SimCardState.PinRequired ->
            "SIM karta je po restartu zamčená — bez PINu nejsou volání, SMS ani mobilní data."
        SimCardState.PukRequired ->
            "SIM vyžaduje PUK kód. Otevřete nastavení SIM karty v systému Androidu."
        SimCardState.NetworkLocked ->
            "SIM je síťově uzamčená — kontaktujte operátora nebo odemkněte v nastavení."
        SimCardState.Absent ->
            "SIM karta není vložená nebo není rozpoznaná."
        SimCardState.NotReady ->
            "SIM se inicializuje — chvíli počkejte, nebo ji znovu odemkněte v nastavení."
        SimCardState.Unknown ->
            "Mobilní síť není dostupná — zkontrolujte SIM kartu v nastavení tabletu."
        SimCardState.Ready, SimCardState.NoTelephony -> ""
    }

    private fun mapSimState(raw: Int): SimCardState = when (raw) {
        TelephonyManager.SIM_STATE_ABSENT -> SimCardState.Absent
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> SimCardState.PinRequired
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> SimCardState.PukRequired
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> SimCardState.NetworkLocked
        TelephonyManager.SIM_STATE_READY -> SimCardState.Ready
        TelephonyManager.SIM_STATE_NOT_READY,
        TelephonyManager.SIM_STATE_CARD_IO_ERROR,
        TelephonyManager.SIM_STATE_CARD_RESTRICTED,
        -> SimCardState.NotReady
        TelephonyManager.SIM_STATE_PERM_DISABLED -> SimCardState.Absent
        TelephonyManager.SIM_STATE_UNKNOWN -> SimCardState.Unknown
        else -> SimCardState.Unknown
    }

    private fun intentForComponent(pkg: String, cls: String): Intent? =
        runCatching {
            Intent().setComponent(ComponentName(pkg, cls))
        }.getOrNull()
}
