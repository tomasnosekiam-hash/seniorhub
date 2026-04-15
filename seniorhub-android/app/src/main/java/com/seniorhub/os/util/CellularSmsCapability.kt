package com.seniorhub.os.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager

/**
 * Zda má smysl zkusit **klasickou SMS přes mobilní síť** ([SmsManager]).
 * Bez telefonního HW, bez SIM nebo bez [SmsManager] použijeme zápis do Firestore (vzkaz rodině v aplikaci).
 */
object CellularSmsCapability {

    fun canSendCellularSms(context: Context): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return false
        }
        if (smsManager(context) == null) {
            return false
        }
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java) ?: return false
            tm.simState == TelephonyManager.SIM_STATE_READY
        } catch (_: SecurityException) {
            true
        }
    }

    private fun smsManager(context: Context): SmsManager? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)?.let { return it }
        }
        @Suppress("DEPRECATION")
        return SmsManager.getDefault()
    }
}
