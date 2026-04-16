package com.seniorhub.os.matej

import com.seniorhub.os.data.Contact
import java.util.Locale

/**
 * Spárování textu z modelu (např. „babička“, „Tom“) na [Contact] ze seznamu na zařízení.
 */
fun matchContactByQuery(query: String?, contacts: List<Contact>): Contact? {
    val q = query?.trim()?.lowercase(CS)?.takeIf { it.isNotEmpty() } ?: return null
    if (contacts.isEmpty()) return null

    val exact = contacts.filter { it.name.trim().equals(q, ignoreCase = true) }
    if (exact.size == 1) return exact.first()

    val contains = contacts.filter {
        val n = it.name.trim().lowercase(CS)
        n.contains(q) || q.contains(n)
    }
    if (contains.size == 1) return contains.first()
    if (contains.isNotEmpty()) {
        return contains.minWith(
            compareBy<Contact> { it.name.length }.thenBy { !it.isEmergency },
        )
    }

    val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
    if (tokens.isEmpty()) return null
    return contacts.firstOrNull { c ->
        val n = c.name.lowercase(CS)
        tokens.any { t -> n.contains(t) }
    }
}

private val CS = Locale.forLanguageTag("cs-CZ")
