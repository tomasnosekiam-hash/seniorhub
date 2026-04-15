package com.seniorhub.os.util

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

/**
 * Odeslání SMS přes systémový [SmsManager] (vyžaduje [android.Manifest.permission.SEND_SMS]).
 */
object SmsSender {
    private const val TAG = "SmsSender"

    fun send(context: Context, phone: String, body: String): Result<Unit> {
        val dest = normalizePhoneForDial(phone)
            ?: return Result.failure(IllegalArgumentException("Neplatné telefonní číslo."))
        val text = body.trim()
        if (text.isEmpty()) return Result.failure(IllegalArgumentException("Zpráva je prázdná."))

        return try {
            val sm = smsManager(context)
                ?: return Result.failure(
                    IllegalStateException(
                        "SMS na tomto zařízení není k dispozici (SmsManager). Zkus jiné zařízení nebo SIM.",
                    ),
                )
            @Suppress("DEPRECATION")
            val parts = sm.divideMessage(text)
            if (parts.size <= 1) {
                sm.sendTextMessage(dest, null, text, null, null)
            } else {
                sm.sendMultipartTextMessage(dest, null, parts, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "send failed", e)
            Result.failure(e)
        }
    }

    /**
     * [Context.getSystemService] pro [SmsManager] je od API 31 preferovaná cesta, ale na emulátoru
     * nebo bez telefonního subsystému může vrátit **null** — pak použijeme [SmsManager.getDefault].
     */
    private fun smsManager(context: Context): SmsManager? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)?.let { return it }
        }
        @Suppress("DEPRECATION")
        return SmsManager.getDefault()
    }
}
