package com.seniorhub.os.matej

import android.content.Context
import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "MatejCompositeBrain"
private const val GEMINI_TIMEOUT_MS = 22_000L

/**
 * Pořadí: **Gemini Nano** (on-device) → při síti a klíči **Gemini Flash** (cloud) → **heuristiky**.
 */
internal class MatejCompositeBrain(
    private val appContext: Context,
    private val heuristic: MatejHeuristicBrain,
    private val nano: MatejNanoBrain,
    private val flash: MatejGeminiFlashBrain?,
) : MatejBrain {

    override suspend fun decide(input: MatejBrainInput): MatejTurnOutcome {
        val nanoOutcome = runCatching { nano.decideIfAvailable(input) }
            .onFailure { Log.w(TAG, "Nano vrstva selhala", it) }
            .getOrNull()
        if (nanoOutcome != null) {
            Log.i(TAG, "výsledek z Gemini Nano (on-device) — bez cloudu")
            return nanoOutcome
        }

        val flashBrain = flash
        if (flashBrain == null || !appContext.hasValidatedInternet()) {
            Log.i(TAG, "fallback: heuristiky (Nano nedostupný nebo chybí síť/klíč pro Flash)")
            return heuristic.decide(input)
        }
        Log.i(TAG, "Nano nedostupný — zkouším Gemini Flash (cloud)")
        val geminiResult = withTimeoutOrNull(GEMINI_TIMEOUT_MS) {
            runCatching { flashBrain.decide(input) }
                .onFailure { Log.w(TAG, "Gemini Flash selhalo", it) }
                .getOrNull()
        }
        return geminiResult ?: heuristic.decide(input)
    }
}
