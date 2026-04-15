package com.seniorhub.os.matej

import java.text.Normalizer
import java.util.Locale

/**
 * Porovnání přepisu STT s jménem asistenta z cloudu (např. Matěj / Matěji).
 */
internal fun normalizeCs(s: String): String =
    Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)

/**
 * Vrací true, pokud přepis odpovídá oslovení asistenta (obsahuje kořen jména).
 */
internal fun matchesAssistantWake(transcript: String, assistantNameFromConfig: String): Boolean {
    val spoken = normalizeCs(transcript)
    if (spoken.isBlank()) return false
    val name = normalizeCs(assistantNameFromConfig)
    if (name.length >= 3 && spoken.contains(name)) return true
    // Oslovení v 2. pádě / bez diakritiky: "mateji" obsahuje "matej"
    val root = name.trimEnd('i').trimEnd('e')
    if (root.length >= 3 && spoken.contains(root)) return true
    return false
}
