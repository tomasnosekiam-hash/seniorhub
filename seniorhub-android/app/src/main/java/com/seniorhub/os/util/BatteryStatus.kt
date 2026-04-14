package com.seniorhub.os.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStatus(
    /** 0–100, nebo `null` pokud systém nevrátí úroveň. */
    val percent: Int?,
    val charging: Boolean,
)

/**
 * Okamžitý stav baterie (sticky broadcast) — vhodné pro zápis heartbeatu do Firestore.
 */
fun readBatteryStatus(context: Context): BatteryStatus {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val status = context.registerReceiver(null, filter) ?: return BatteryStatus(percent = null, charging = false)
    val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val pct = if (level >= 0 && scale > 0) {
        (level * 100 / scale).coerceIn(0, 100)
    } else {
        null
    }
    val chargePlug = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val batStatus = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = batStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
        batStatus == BatteryManager.BATTERY_STATUS_FULL ||
        chargePlug != 0
    return BatteryStatus(percent = pct, charging = charging)
}
