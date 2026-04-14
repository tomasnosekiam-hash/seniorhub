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

    private fun smsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }
}
