package com.seniorhub.os.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log

/**
 * Normalizace telefonního čísla pro `tel:` (číslice a volitelné úvodní +).
 */
fun normalizePhoneForDial(phone: String): String? {
    val buf = StringBuilder()
    for (c in phone) {
        when {
            c.isDigit() -> buf.append(c)
            c == '+' && buf.isEmpty() -> buf.append(c)
        }
    }
    val s = buf.toString()
    if (s.isEmpty() || !s.any { it.isDigit() }) return null
    return s
}

/** Jen číslice (bez +) — pro porovnání čísel z SMS/RCS vs. kontaktů uložených různě. */
fun phoneDigitsOnly(phone: String): String? =
    normalizePhoneForDial(phone)?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }

/**
 * Shoda čísel pro vlákno / příchozí SMS–RCS (např. `+420777…` vs. `777…` nebo `00420…`).
 */
fun phonesMatchForThread(incoming: String, contactPhone: String): Boolean {
    val a = normalizePhoneForDial(incoming) ?: return false
    val b = normalizePhoneForDial(contactPhone) ?: return false
    if (a == b) return true
    val da = phoneDigitsOnly(incoming) ?: return false
    val db = phoneDigitsOnly(contactPhone) ?: return false
    if (da == db) return true
    val ca = czechMobileCore(da)
    val cb = czechMobileCore(db)
    if (ca != null && cb != null && ca == cb) return true
    if (da.endsWith(db) || db.endsWith(da)) {
        val shorter = minOf(da.length, db.length)
        if (shorter >= 9) return true
    }
    return false
}

/** 9 číslic českého mobilu bez předvolby 420. */
private fun czechMobileCore(digits: String): String? {
    val d = digits.trimStart('0')
    return when {
        d.length == 9 && d.startsWith('7') -> d
        d.startsWith("420") && d.length >= 12 -> d.drop(3).take(9).takeIf { it.length == 9 }
        d.length > 9 -> d.takeLast(9).takeIf { it.length == 9 && it.startsWith('7') }
        else -> null
    }
}

fun dialIntent(phone: String): Intent? {
    val normalized = normalizePhoneForDial(phone) ?: return null
    val uri = Uri.fromParts("tel", normalized, null)
    return Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

fun dialPadIntent(phone: String): Intent? {
    val normalized = normalizePhoneForDial(phone) ?: return null
    val uri = Uri.fromParts("tel", normalized, null)
    return Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

fun Context.startOutgoingCall(phone: String): Boolean {
    val normalized = normalizePhoneForDial(phone) ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val telecom = getSystemService(TelecomManager::class.java)
        if (telecom != null) {
            return try {
                telecom.placeCall(Uri.fromParts("tel", normalized, null), Bundle.EMPTY)
                true
            } catch (e: Exception) {
                Log.w("PhoneDialer", "TelecomManager.placeCall failed", e)
                false
            }
        }
    }
    val intent = dialIntent(normalized) ?: return false
    return try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        // SecurityException if CALL_PHONE denied/revoked; avoid crashing the app (e.g. race with AppOps).
        Log.w("PhoneDialer", "startOutgoingCall failed", e)
        false
    }
}

fun Context.openDialPad(phone: String): Boolean {
    val intent = dialPadIntent(phone) ?: return false
    startActivity(intent)
    return true
}
