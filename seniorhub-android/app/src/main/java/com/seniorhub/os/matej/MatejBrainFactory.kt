package com.seniorhub.os.matej

import android.content.Context
import com.seniorhub.os.BuildConfig

/**
 * Složení [MatejBrain]: **Gemini Nano** (on-device) → cloud Gemini ([BuildConfig.GEMINI_CLOUD_MODEL]) → heuristiky.
 */
object MatejBrainFactory {

    fun create(appContext: Context): MatejBrain {
        val heuristic = MatejHeuristicBrain()
        val nano = MatejNanoBrain(appContext)
        val key = BuildConfig.GEMINI_API_KEY
        val flash = if (key.isBlank()) {
            null
        } else {
            MatejGeminiFlashBrain(
                apiKey = key,
                modelName = BuildConfig.GEMINI_CLOUD_MODEL,
            )
        }
        return MatejCompositeBrain(appContext, heuristic, nano, flash)
    }
}
