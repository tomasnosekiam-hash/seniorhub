package com.seniorhub.os.matej

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.seniorhub.os.R

/**
 * On-device **Gemini Nano** přes ML Kit Prompt API (Android AICore).
 * Pokud model není stažen nebo zařízení nepodporuje, vrací [decideIfAvailable] `null` a volá se cloud / heuristiky.
 *
 * Viz [Get started with Prompt API](https://developers.google.com/ml-kit/genai/prompt/android/get-started).
 */
internal class MatejNanoBrain(
    appContext: Context,
) {
    private val appContext = appContext.applicationContext

    private val model by lazy { Generation.getClient() }

    /**
     * @return výsledek z Nano, nebo `null` pokud Nano není k dispozici / inference selhala (eslalovat výš).
     */
    suspend fun decideIfAvailable(input: MatejBrainInput): MatejTurnOutcome? {
        val utterance = input.utterance?.trim().orEmpty()
        if (utterance.isEmpty()) {
            // Nechat eskalovat na Flash / heuristiky — stejná odpověď, ale správné logování a cloud když je k dispozici.
            Log.i(TAG, "prázdný přepis — přeskakuji Nano (Flash/heuristiky)")
            return null
        }

        val status = runCatching { model.checkStatus() }
            .onFailure { Log.w(TAG, "checkStatus selhalo", it) }
            .getOrNull() ?: return null

        when (status) {
            FeatureStatus.AVAILABLE -> { /* pokračuj */ }
            FeatureStatus.DOWNLOADABLE ->
                Log.i(TAG, "Gemini Nano je ke stažení (AICore) — po stažení zkus znovu; zatím fallback.")
            FeatureStatus.DOWNLOADING ->
                Log.i(TAG, "Gemini Nano se stahuje…")
            FeatureStatus.UNAVAILABLE ->
                Log.i(TAG, "Gemini Nano na tomto zařízení není dostupný (AICore / konfigurace / OEM).")
            else ->
                Log.i(TAG, "Neočekávaný stav Nano: $status")
        }
        if (status != FeatureStatus.AVAILABLE) return null

        val request = generateContentRequest(TextPart(buildMatejFullPrompt(input))) {
            temperature = 0.25f
            maxOutputTokens = 768
        }
        val response = runCatching {
            model.generateContent(request)
        }.onFailure { e ->
            if (e is GenAiException) {
                Log.w(TAG, "Nano GenAiException: ${e.errorCode}", e)
            } else {
                Log.w(TAG, "Nano generateContent selhalo", e)
            }
        }.getOrNull() ?: return null

        val raw = response.candidates.firstOrNull()?.text?.trim().orEmpty()
        if (raw.isEmpty()) {
            Log.w(TAG, "Prázdná odpověď Nano")
            return null
        }
        Log.i(TAG, "inference Nano dokončena (ML Kit / AICore)")
        return parseMatejBrainJson(raw, input)
    }

    companion object {
        private const val TAG = "MatejNanoBrain"
    }
}
