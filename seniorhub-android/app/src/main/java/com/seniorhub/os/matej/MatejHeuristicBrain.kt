package com.seniorhub.os.matej

import com.seniorhub.os.R
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Záložní pravidla v češtině — výstup přes [MatejTurnOutcome] (záloha za Gemini Flash).
 */
class MatejHeuristicBrain : MatejBrain {

    private val cs = Locale.forLanguageTag("cs-CZ")

    override suspend fun decide(input: MatejBrainInput): MatejTurnOutcome {
        val ctx = input.context
        val u = input.utterance?.trim()?.lowercase(cs) ?: ""
        if (u.isBlank()) {
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_silent))
        }

        val weatherHints = listOf(
            "počas", "pocasi", "venku", "teplot", "déšť", "dest", "prší", "prsi", "oblač", "oblac",
            "jak je na", "jaké je",
        )
        if (weatherHints.any { u.contains(it) }) {
            val line = input.weatherLine?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.matej_reply_weather_unknown)
            return MatejTurnOutcome.Speak(line)
        }

        val timeHints = listOf("hodin", "kolik je", "kolik máme", "čas", "cas", "kolik hodin")
        if (timeHints.any { u.contains(it) } || u == "čas" || u == "cas") {
            val label = input.now.format(DateTimeFormatter.ofPattern("HH:mm"))
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_time, label))
        }

        val helpHints = listOf("pomoc", "co umíš", "co umis", "co můžeš", "co muzes", "co umíte")
        if (helpHints.any { u.contains(it) }) {
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_help))
        }

        val commHints = listOf(
            "zavolej", "zavolat", "volej", "vytoč", "vytoc", "sms", "zprávu", "zpravu", "napiš", "napis",
            "pošli", "posli", "hovor",
        )
        if (commHints.any { u.contains(it) }) {
            val smsHints = listOf("sms", "zprávu", "zpravu", "napiš", "napis", "pošli", "posli")
            val looksSms = smsHints.any { u.contains(it) }
            if (looksSms) {
                val words = u.split(Regex("\\s+")).filter { it.isNotBlank() }
                val hasMessageBody = u.contains(" že ") || u.contains(" ze ") || u.length > 40
                if (words.size <= 7 && !hasMessageBody) {
                    return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_sms_need_details))
                }
            }
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_call_sms))
        }

        return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
    }
}
