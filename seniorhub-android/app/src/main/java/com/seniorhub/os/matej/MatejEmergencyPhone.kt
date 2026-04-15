package com.seniorhub.os.matej

import com.seniorhub.os.data.Contact
import com.seniorhub.os.util.normalizePhoneForDial

internal data class EmergencyDialTarget(
    val rawPhone: String,
    val contactLabel: String?,
)

/**
 * Priorita: první kontakt s `is_emergency` a platným číslem (řazení jako ve Firestore),
 * jinak první platné číslo v seznamu, jinak SIM z konfigurace zařízení.
 */
internal fun pickEmergencyDialTarget(
    contacts: List<Contact>,
    fallbackSimRaw: String,
): EmergencyDialTarget? {
    for (c in contacts) {
        if (!c.isEmergency) continue
        if (normalizePhoneForDial(c.phone) == null) continue
        return EmergencyDialTarget(
            rawPhone = c.phone,
            contactLabel = c.name.trim().takeIf { it.isNotEmpty() },
        )
    }
    for (c in contacts) {
        if (normalizePhoneForDial(c.phone) != null) {
            return EmergencyDialTarget(
                rawPhone = c.phone,
                contactLabel = c.name.trim().takeIf { it.isNotEmpty() },
            )
        }
    }
    val sim = normalizePhoneForDial(fallbackSimRaw) ?: return null
    return EmergencyDialTarget(rawPhone = fallbackSimRaw, contactLabel = null)
}
