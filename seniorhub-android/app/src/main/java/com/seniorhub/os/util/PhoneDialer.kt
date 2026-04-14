package com.seniorhub.os.util

import android.content.Context
import android.content.Intent
import android.net.Uri

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
    val intent = dialIntent(phone) ?: return false
    startActivity(intent)
    return true
}

fun Context.openDialPad(phone: String): Boolean {
    val intent = dialPadIntent(phone) ?: return false
    startActivity(intent)
    return true
}
