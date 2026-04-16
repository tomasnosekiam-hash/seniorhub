package com.seniorhub.os.matej

import java.util.Locale

/** Jednoduché rozpoznání souhlasu / nesouhlasu po TTS (ano/ne k SMS nebo hovoru). */
object MatejConfirmationPhrases {

    private val cs = Locale.forLanguageTag("cs-CZ")

    fun isAffirmative(raw: String?): Boolean {
        val u = raw?.trim()?.lowercase(cs) ?: return false
        if (u.isEmpty()) return false
        val tokens = listOf(
            "ano", "jo", "jasně", "jasne", "jojo", "ok", "okej", "dobře", "dobre",
            "souhlasím", "souhlasim", "můžeš", "muzes", "proveď", "proved", "pošli", "posli",
        )
        return tokens.any { u == it || u.startsWith("$it ") || u.endsWith(" $it") } ||
            (u.length <= 12 && tokens.any { u.contains(it) })
    }

    fun isNegative(raw: String?): Boolean {
        val u = raw?.trim()?.lowercase(cs) ?: return false
        if (u.isEmpty()) return false
        val tokens = listOf(
            "ne", "né", "nechci", "zruš", "zrus", "stop", "nic", "raději ne", "radši ne",
        )
        return tokens.any { u == it || u.startsWith(it) }
    }

    /**
     * Ukončení celé relace (ne potvrzení akce) — před voláním mozku v hlavní smyčce.
     */
    fun looksLikeSessionEnd(raw: String?): Boolean {
        val u = raw?.trim()?.lowercase(cs) ?: return false
        if (u.isEmpty()) return false
        val phrases = listOf(
            "konec",
            "nashle",
            "na shle",
            "sbohem",
            "díky nashle",
            "diky nashle",
            "děkuju na shle",
            "dekuju na shle",
            "už nic",
            "uz nic",
            "to stačí",
            "to staci",
            "dost už",
            "dost uz",
            "ukonči asistenta",
            "ukonci asistenta",
            "zavři asistenta",
            "zavri asistenta",
            "stop",
        )
        if (phrases.any { u == it }) return true
        return phrases.any { p ->
            u.startsWith("$p ") || u.endsWith(" $p") || u.contains(" $p ")
        }
    }
}
