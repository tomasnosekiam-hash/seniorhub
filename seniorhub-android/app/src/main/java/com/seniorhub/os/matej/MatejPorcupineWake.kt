package com.seniorhub.os.matej

import android.content.Context
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.seniorhub.os.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Blokuje, dokud Porcupine nerozpozná wake word, nebo nevrátí chybu (`false`).
 *
 * - V `assets/porcupine/` lze přidat vlastní `*.ppn` + odpovídající `*.pv` (např. čeština z Picovoice Console).
 * - Bez těchto souborů se použije vestavěné anglické slovo **„porcupine“** (vhodné pro test; produkce = vlastní .ppn).
 */
internal suspend fun awaitPorcupineWakeWord(context: Context): Boolean {
    if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) return false
    return suspendCancellableCoroutine { cont ->
        var manager: PorcupineManager? = null
        val callback = PorcupineManagerCallback { _: Int ->
            runCatching {
                manager?.stop()
                manager?.delete()
            }
            manager = null
            if (cont.isActive) cont.resume(true)
        }
        try {
            val builder = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setSensitivity(0.65f)
            val names = try {
                context.assets.list("porcupine")?.toList().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            val ppn = names.firstOrNull { it.endsWith(".ppn", ignoreCase = true) }
            val pv = names.firstOrNull { it.endsWith(".pv", ignoreCase = true) }
            if (ppn != null && pv != null) {
                builder.setKeywordPath("porcupine/$ppn")
                builder.setModelPath("porcupine/$pv")
            } else {
                builder.setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
            }
            manager = builder.build(context.applicationContext, callback)
            manager?.start()
        } catch (_: PorcupineException) {
            runCatching {
                manager?.stop()
                manager?.delete()
            }
            manager = null
            if (cont.isActive) cont.resume(false)
        } catch (_: Throwable) {
            runCatching {
                manager?.stop()
                manager?.delete()
            }
            manager = null
            if (cont.isActive) cont.resume(false)
        }
        cont.invokeOnCancellation {
            runCatching {
                manager?.stop()
                manager?.delete()
            }
            manager = null
        }
    }
}
