package com.seniorhub.os.matej

import android.content.Context
import com.seniorhub.os.data.Contact
import java.time.LocalTime

/**
 * Jedno kolo konverzace (uživatel → odpověď asistenta po zpracování).
 * Slouží jako kontext pro cloud / on-device LLM v rámci jedné relace.
 */
data class MatejConversationTurn(
    val userText: String,
    val assistantText: String,
)

/**
 * Vstup do rozhodovací vrstvy Matěje (heuristiky / Gemini / později Nano přes stejné rozhraní).
 */
data class MatejBrainInput(
    val context: Context,
    val utterance: String?,
    val weatherLine: String?,
    val now: LocalTime,
    val contacts: List<Contact>,
    /** Poslední kola v aktuální relaci (nejnovější nahoře nebo dole — viz [com.seniorhub.os.matej.MatejBrainPrompt]). */
    val conversationHistory: List<MatejConversationTurn> = emptyList(),
)

/**
 * Výsledek jednoho „tahu“ po přepsání řeči — buď jen TTS, nebo bezpečnostní potvrzení SMS / hovoru.
 */
sealed class MatejTurnOutcome {
    data class Speak(val text: String) : MatejTurnOutcome()

    data class ConfirmSendSms(
        val contact: Contact,
        val body: String,
        /** Kompletní věta k TTS včetně výzvy k ano/ne. */
        val promptSpoken: String,
    ) : MatejTurnOutcome()

    data class ConfirmCall(
        val contact: Contact,
        val promptSpoken: String,
    ) : MatejTurnOutcome()
}

/**
 * Rozhraní „mozku“ Matěje — [MatejBrainFactory] skládá Nano (on-device), Gemini Flash (cloud) a heuristiky.
 */
interface MatejBrain {
    suspend fun decide(input: MatejBrainInput): MatejTurnOutcome
}
