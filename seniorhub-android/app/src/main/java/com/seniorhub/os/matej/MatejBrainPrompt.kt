package com.seniorhub.os.matej

import android.util.Log
import com.seniorhub.os.R
import com.seniorhub.os.util.normalizePhoneForDial
import java.time.format.DateTimeFormatter
import org.json.JSONObject

/**
 * Sdílený systémový prompt a parsování JSON výstupu pro cloud Gemini Flash i on-device Gemini Nano (ML Kit).
 */
internal val MatejBrainSystemPrompt: String = """
    Jsi hlasový asistent Matěj pro české seniory — klidný, vlídný společník. Odpovídáš výhradně jedním JSON objektem (žádný markdown, žádný komentář).

    Schéma:
    - kind: řetězec — "reply" | "confirm_sms" | "confirm_call"
    - spoken_text: český text k přečtení nahlas (stručně, přirozeně, bez robotických formulací typu „jsem virtuální asistent“)
    - contact_query: volitelně část jména kontaktu ze seznamu (pro confirm_*)
    - sms_body: text SMS (jen pro confirm_sms)

    Pravidla:
    - Nemluv o sobě jako o „virtuálním asistentovi“ ani se nepředstavuj šablonovitě — odpovídej přirozeně jako společník, bez metapředstavování.
    - Dialog může mít více kol — zohledni dodanou předchozí část konverzace; nenavrhuj zbytečně opakovat totéž.
    - Otázky na počasí nebo čas: kind="reply", v spoken_text použij dodaný řádek počasí / čas z uživatelské zprávy; nevymýšlej data.
    - SMS / zpráva: kind="confirm_sms" jen když máš jistého příjemce (contact_query sedí na jeden kontakt ze seznamu) a skutečný text zprávy v sms_body (aspoň pár slov). Chybí-li komu nebo co napsat, použij kind="reply": v spoken_text se krátce zeptej a řekni, co ještě potřebuješ — a vždy přidej příklad věty, jak to má uživatel říct nahlas (např. „pošli SMS Janě, že …“). U tohoto typu odpovědi nesmí být spoken_text prázdný.
    - Pokud chce zavolat: kind="confirm_call" jen se správným contact_query; jinak kind="reply" s otázkou po jménu z kontaktů; spoken_text neprázdný.
    - Nikdy nevymýšlej telefonní čísla — jen kontakty ze seznamu v uživatelské zprávě.
    - Pro kind "confirm_call" a "confirm_sms": aplikace sama přečte otázku na ano/ne — do spoken_text NIKDY nepiš, že už voláš/odesíláš (např. „volám“, „vytáčím“). U úplného confirm_* může být spoken_text prázdný. Pokud sms_body chybí nebo je nejasný příjemce, nepoužívej confirm_sms — vrať kind="reply" s doplněním údajů (viz SMS výše).
""".trimIndent()

/** Kolik posledních kol poslat do promptu (tokeny / stabilita). */
internal const val MATEJ_HISTORY_MAX_TURNS = 8

internal fun buildMatejUserPromptBlock(input: MatejBrainInput): String {
    val utterance = input.utterance?.trim().orEmpty()
    val timeLabel = input.now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val contactsBlock = input.contacts.joinToString("\n") { c ->
        "- ${c.name.trim()} | ${c.phone.trim()}"
    }.ifBlank { "(žádné kontakty)" }
    val historyTail = input.conversationHistory.takeLast(MATEJ_HISTORY_MAX_TURNS)
    return buildString {
        if (historyTail.isNotEmpty()) {
            appendLine("Předchozí část dialogu (od staršího k novějšímu):")
            for (t in historyTail) {
                val u = t.userText.trim().replace("\n", " ")
                val a = t.assistantText.trim().replace("\n", " ")
                appendLine("- Uživatel: $u")
                appendLine("  Matěj: $a")
            }
            appendLine()
        }
        appendLine("Přepis uživatele (čeština): $utterance")
        appendLine("Aktuální čas: $timeLabel")
        appendLine("Řádek počasí (může být prázdný): ${input.weatherLine ?: ""}")
        appendLine("Seznam kontaktů (používej jen tato čísla; nevymýšlej):")
        appendLine(contactsBlock)
        appendLine()
        appendLine("Výstup: pouze jeden JSON objekt podle systémových instrukcí, bez markdownu.")
    }
}

internal fun buildMatejFullPrompt(input: MatejBrainInput): String =
    "${MatejBrainSystemPrompt}\n\n---\n${buildMatejUserPromptBlock(input)}"

internal fun parseMatejBrainJson(raw: String, input: MatejBrainInput): MatejTurnOutcome {
    val ctx = input.context
    val t = raw.trim()
    if (t.isEmpty()) {
        Log.w(TAG_PARSE, "Prázdná odpověď modelu")
        return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
    }
    return try {
        parseJsonOutcome(extractJsonObject(t), input)
    } catch (e: Exception) {
        Log.w(TAG_PARSE, "Parsování JSON z modelu", e)
        MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
    }
}

private fun parseJsonOutcome(json: JSONObject, input: MatejBrainInput): MatejTurnOutcome {
    val ctx = input.context
    val kind = json.optString("kind").trim().lowercase()
    val spoken = json.optString("spoken_text").trim()
    val query = json.optString("contact_query").trim()
    val smsBody = json.optString("sms_body").trim()

    fun speakOrFallback(): MatejTurnOutcome {
        if (spoken.isNotEmpty()) return MatejTurnOutcome.Speak(spoken)
        return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
    }

    when (kind) {
        "confirm_sms" -> {
            if (smsBody.isEmpty()) {
                if (spoken.isNotEmpty()) return MatejTurnOutcome.Speak(spoken)
                return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_sms_need_details))
            }
            val contact = matchContactByQuery(query, input.contacts)
                ?: return MatejTurnOutcome.Speak(
                    spoken.ifEmpty { ctx.getString(R.string.matej_brain_contact_not_found) },
                )
            if (normalizePhoneForDial(contact.phone) == null) {
                return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_brain_invalid_phone))
            }
            // Vždy pevná otázka na ano/ne — LLM často v spoken_text halucinuje „už odesílám“, což odporuje UI.
            val prompt = ctx.getString(R.string.matej_confirm_sms_prompt, contact.name, smsBody)
            return MatejTurnOutcome.ConfirmSendSms(contact, smsBody, prompt)
        }
        "confirm_call" -> {
            val contact = matchContactByQuery(query, input.contacts)
                ?: return MatejTurnOutcome.Speak(
                    spoken.ifEmpty { ctx.getString(R.string.matej_brain_contact_not_found) },
                )
            if (normalizePhoneForDial(contact.phone) == null) {
                return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_brain_invalid_phone))
            }
            val prompt = ctx.getString(R.string.matej_confirm_call_prompt, contact.name)
            return MatejTurnOutcome.ConfirmCall(contact, prompt)
        }
        else -> {
            if (spoken.isNotEmpty()) return MatejTurnOutcome.Speak(spoken)
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
        }
    }
}

private fun extractJsonObject(raw: String): JSONObject {
    var t = raw.trim()
    if (t.startsWith("```")) {
        t = t.removePrefix("```json").removePrefix("```").trim()
        val end = t.lastIndexOf("```")
        if (end >= 0) t = t.substring(0, end).trim()
    }
    return JSONObject(t)
}

private const val TAG_PARSE = "MatejBrainPrompt"
