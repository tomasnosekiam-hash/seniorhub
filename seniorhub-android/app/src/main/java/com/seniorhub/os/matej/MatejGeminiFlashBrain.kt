package com.seniorhub.os.matej

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.seniorhub.os.R

/**
 * Cloud Gemini (Flash) — strukturovaný JSON; při chybě parsování / sítě nechte volat [MatejHeuristicBrain] zvenku.
 *
 * **Produkt:** primární inteligence má běžet on-device jako **Gemini Nano** ([MatejNanoBrain]); tato třída je cloudová eskalace
 * (min. 2.5 Flash — viz `BuildConfig.GEMINI_CLOUD_MODEL`).
 */
class MatejGeminiFlashBrain(
    private val apiKey: String,
    private val modelName: String,
) : MatejBrain {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.25f
                // 768 stačilo málo — JSON + čeština občas končily MAX_TOKENS (uříznutá odpověď, výjimka v klientu).
                maxOutputTokens = 2048
            },
        )
    }

    override suspend fun decide(input: MatejBrainInput): MatejTurnOutcome {
        val ctx = input.context
        val utterance = input.utterance?.trim().orEmpty()
        if (utterance.isEmpty()) {
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_silent))
        }

        val response = model.generateContent(buildMatejFullPrompt(input))
        val raw = response.text?.trim().orEmpty()
        if (raw.isEmpty()) {
            Log.w(TAG, "Prázdná odpověď modelu")
            return MatejTurnOutcome.Speak(ctx.getString(R.string.matej_reply_fallback))
        }
        return parseMatejBrainJson(raw, input)
    }

    companion object {
        private const val TAG = "MatejGeminiFlash"
    }
}
