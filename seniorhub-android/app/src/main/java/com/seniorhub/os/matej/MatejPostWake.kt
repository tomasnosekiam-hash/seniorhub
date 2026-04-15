package com.seniorhub.os.matej

import com.seniorhub.os.data.Contact
import com.seniorhub.os.util.normalizePhoneForDial

/**
 * Po probuzení (jménem asistenta / Porcupine) jedno krátké naslouchání STT — klasifikace příkazu.
 */
internal sealed class MatejPostWakeAction {
    data object Emergency : MatejPostWakeAction()

    data object Weather : MatejPostWakeAction()

    /** Nepřečtený vzkaz z webu (bez `delivery`). */
    data object ReadFamilyMessage : MatejPostWakeAction()

    data class DialContact(val contact: Contact) : MatejPostWakeAction()

    /** Druhé STT = text SMS, pak odeslání (síť / Firestore jako na dashboardu). */
    data class SendSms(val contact: Contact) : MatejPostWakeAction()

    data object SmsNeedRecipient : MatejPostWakeAction()

    data object DialNeedContact : MatejPostWakeAction()
}

/**
 * Pořadí: počasí → čtení vzkazu → SMS (klíčová slova + kontakt) → hovor (klíčová slova + kontakt)
 * → jen jméno kontaktu (vytočení) → nouze.
 */
internal fun classifyPostWakeCommand(
    transcript: String,
    contacts: List<Contact>,
): MatejPostWakeAction {
    val t = normalizeCs(transcript)
    if (t.isBlank()) return MatejPostWakeAction.Emergency

    if (matchesWeatherIntent(t)) return MatejPostWakeAction.Weather

    if (matchesReadMessageIntent(t)) return MatejPostWakeAction.ReadFamilyMessage

    if (matchesSmsIntent(t)) {
        findContactByTranscript(t, contacts)?.let { c ->
            return MatejPostWakeAction.SendSms(c)
        }
        return MatejPostWakeAction.SmsNeedRecipient
    }

    if (matchesDialIntent(t)) {
        findContactByTranscript(t, contacts)?.let { c ->
            return MatejPostWakeAction.DialContact(c)
        }
        return MatejPostWakeAction.DialNeedContact
    }

    findContactByTranscript(t, contacts)?.let { c ->
        return MatejPostWakeAction.DialContact(c)
    }

    return MatejPostWakeAction.Emergency
}

private fun matchesWeatherIntent(normalized: String): Boolean {
    val phrases = listOf(
        "pocasi",
        "jak je venku",
        "venku je",
        "kolik stupnu",
        "kolik je stupnu",
        "teplota",
        "predpoved",
        "bude prset",
        "prsi",
        "jak je zima",
        "jak je leto",
        "pocasi dnes",
    )
    return phrases.any { normalized.contains(it) }
}

private fun matchesReadMessageIntent(normalized: String): Boolean {
    val phrases = listOf(
        "precet zpravu",
        "precet vzkaz",
        "precti zpravu",
        "precti vzkaz",
        "prectist zpravu",
        "preci zpravu",
        "preci vzkaz",
        "novy vzkaz",
        "co mi napsali",
        "zprava od rodiny",
        "vzkaz od rodiny",
        "precet novy",
        "precti novy",
        "je nejaka zprava",
        "mas zpravy",
        "mam zpravu",
        "nejaka zprava",
    )
    return phrases.any { normalized.contains(it) }
}

private fun matchesSmsIntent(normalized: String): Boolean {
    val phrases = listOf(
        "sms",
        "napis zpravu",
        "napis sms",
        "odesli zpravu",
        "odesli sms",
        "posli sms",
        "posli zpravu",
        "textovat",
        "smsku",
    )
    return phrases.any { normalized.contains(it) }
}

private fun matchesDialIntent(normalized: String): Boolean {
    val phrases = listOf(
        "zavolej",
        "vytoč",
        "vytoc",
        "volej",
        "zavolat",
        "zavolej mi",
        "spoj me s",
    )
    return phrases.any { normalized.contains(it) }
}

/**
 * Nejdelší jméno první — lepší rozlišení „Jan“ vs „Jana“ při částečné shodě.
 */
internal fun findContactByTranscript(
    normalizedTranscript: String,
    contacts: List<Contact>,
): Contact? {
    val valid = contacts.filter { normalizePhoneForDial(it.phone) != null && it.name.isNotBlank() }
    val sorted = valid.sortedByDescending { normalizeCs(it.name).length }
    for (c in sorted) {
        val name = normalizeCs(c.name)
        if (name.length < 2) continue
        if (normalizedTranscript.contains(name)) return c
        val words = name.split(' ').map { it.trim() }.filter { it.length >= 3 }
        for (w in words) {
            if (normalizedTranscript.contains(w)) return c
        }
    }
    return null
}
