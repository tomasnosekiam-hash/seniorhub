package com.seniorhub.os.matej

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.seniorhub.os.R
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Jednorázové TTS a STT pro relaci Matěje; rozhodnutí textu / potvrzení z [com.seniorhub.os.matej.MatejBrain]. */
object MatejVoicePipeline {

    /** Horní hranice jednoho pokusu STT (ERROR_NO_MATCH často přijde dřív). */
    private const val LISTEN_TIMEOUT_MS = 18_000L
    /** Po ERROR_NO_MATCH / prázdném výsledku — druhý a třetí pokus bez nového spuštění relace. */
    private const val STT_RETRY_ATTEMPTS = 3
    private const val STT_RETRY_GAP_MS = 320L
    private const val UTTERANCE_GREETING = "matej_greeting"
    private const val UTTERANCE_REPLY = "matej_reply"

    /** Preferovaný engine (Google hlasová data / neurální čeština podle nastavení zařízení). */
    private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"

    /** Jeden tag pro Logcat — filtr `Matej` zachytí STT i další řádky z tohoto modulu. */
    private const val TAG = "Matej"

    private fun TextToSpeech.applyMatejVoiceSettings() {
        language = Locale.forLanguageTag("cs-CZ")
        // Nižší tempo = méně „strojové“ vnímání u výchozího systémového hlasu (plně přirozený hlas = nastavení systému / jiný engine).
        setSpeechRate(0.88f)
        setPitch(1.0f)
    }

    /**
     * Vybere nejvhodnější český [Voice] z nabídky engine (bez placeného API) — upřednostní vyšší
     * [Voice.getQuality] a typicky síťové / rozšířené Google hlasy (`network`, `cs-cz-x-…`).
     */
    private fun TextToSpeech.applyMatejVoiceSelection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val list = voices ?: return
        val csVoices = list.filter { v ->
            val loc = v.locale
            loc != null && loc.language == "cs"
        }
        if (csVoices.isEmpty()) {
            Log.w(TAG, "TTS: v engine nejsou žádné české hlasy — zůstává setLanguage(cs-CZ)")
            return
        }
        val best = csVoices.maxWith(
            compareBy<Voice> { v -> voiceSortKey(v) }
                .thenBy { v -> v.name },
        )
        when (val code = setVoice(best)) {
            TextToSpeech.SUCCESS ->
                Log.i(TAG, "TTS: vybrán hlas ${best.name} (locale=${best.locale})")
            else ->
                Log.w(TAG, "TTS: setVoice selhalo ($code), hlas ${best.name}")
        }
    }

    private fun voiceSortKey(v: Voice): Int {
        var k = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            k += v.quality
        }
        val n = v.name.lowercase(Locale.ROOT)
        // Google často označuje neurální / vyšší kvalitu jako network; rozšířené názvy cs-cz-x-…
        if (n.contains("network")) k += 600
        if (n.contains("cs-cz-x-")) k += 400
        if (n.contains("premium") || n.contains("neural")) k += 200
        return k
    }

    private fun TextToSpeech.configureMatejTts() {
        applyMatejVoiceSettings()
        applyMatejVoiceSelection()
    }

    private suspend fun runMatejTtsSession(
        app: Context,
        utteranceId: String,
        text: String,
    ) {
        suspendCoroutine { cont ->
            var tts: TextToSpeech? = null
            val finished = AtomicBoolean(false)

            fun finishTtsOnce() {
                if (!finished.compareAndSet(false, true)) return
                tts?.stop()
                tts?.shutdown()
                tts = null
                cont.resume(Unit)
            }

            fun beginSpeak(instance: TextToSpeech) {
                instance.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            finishTtsOnce()
                        }

                        @SuppressLint("Deprecated")
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            finishTtsOnce()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            finishTtsOnce()
                        }
                    },
                )
                instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }

            fun tryInit(engine: String?) {
                tts?.shutdown()
                val listener = TextToSpeech.OnInitListener { status ->
                    if (status != TextToSpeech.SUCCESS) {
                        Log.w(
                            TAG,
                            "TTS: inicializace selhala (${if (engine != null) "engine=$engine" else "výchozí engine"})",
                        )
                        if (engine != null) {
                            tryInit(null)
                        } else {
                            if (!finished.compareAndSet(false, true)) return@OnInitListener
                            cont.resume(Unit)
                        }
                        return@OnInitListener
                    }
                    val instance = tts ?: return@OnInitListener
                    instance.configureMatejTts()
                    beginSpeak(instance)
                }
                tts = if (engine != null) {
                    TextToSpeech(app, listener, engine)
                } else {
                    TextToSpeech(app, listener)
                }
            }

            tryInit(GOOGLE_TTS_ENGINE)
        }
    }

    suspend fun speakGreeting(context: Context) {
        val app = context.applicationContext
        val text = app.getString(R.string.matej_tts_greeting)
        runMatejTtsSession(app, UTTERANCE_GREETING, text)
    }

    /** Přečte libovolný text (např. odpověď Matěje). */
    suspend fun speakText(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        runMatejTtsSession(context.applicationContext, UTTERANCE_REPLY, trimmed)
    }

    /**
     * Jedno rozpoznání řeči (český jazyk). Při timeoutu [LISTEN_TIMEOUT_MS] vrátí null.
     * Běží na hlavním vlákně kvůli [SpeechRecognizer].
     */
    suspend fun listenOnceCsOrTimeout(context: Context): String? =
        withContext(Dispatchers.Main) {
            repeat(STT_RETRY_ATTEMPTS) { attempt ->
                val text = try {
                    withTimeout(LISTEN_TIMEOUT_MS) {
                        listenOnceCsMain(context.applicationContext)
                    }
                } catch (_: TimeoutCancellationException) {
                    Log.w(TAG, "STT: pokus ${attempt + 1}/$STT_RETRY_ATTEMPTS — překročen čas ${LISTEN_TIMEOUT_MS} ms")
                    null
                }
                if (!text.isNullOrBlank()) return@withContext text
                if (attempt < STT_RETRY_ATTEMPTS - 1) {
                    Log.w(TAG, "STT: prázdný výsledek — opakuji pokus ${attempt + 2}/$STT_RETRY_ATTEMPTS")
                    delay(STT_RETRY_GAP_MS)
                }
            }
            null
        }

    private suspend fun listenOnceCsMain(context: Context): String? =
        suspendCancellableCoroutine { cont ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "STT: SpeechRecognizer na zařízení nedostupný")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            Log.i(TAG, "STT: startListening (cs-CZ)")
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.i(TAG, "STT: onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.i(TAG, "STT: onBeginningOfSpeech (mikrofon zachytil řeč)")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.i(TAG, "STT: onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "STT: onError ${speechErrorLabel(error)}")
                    recognizer.destroy()
                    cont.resume(null)
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    recognizer.destroy()
                    if (text.isNullOrBlank()) {
                        Log.w(TAG, "STT: onResults — prázdný přepis (žádný text)")
                        cont.resume(null)
                    } else {
                        val preview = if (text.length > 120) text.take(120) + "…" else text
                        Log.i(TAG, "STT: onResults — délka ${text.length}: $preview")
                        cont.resume(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            recognizer.setRecognitionListener(listener)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                // Bez toho OEM/Google UI často vypadá, že jde „namluvit jednu konkrétní frázi“ — jde o volnou češtinu.
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.matej_stt_prompt))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Delší tolerance ticha — méně předčasných ERROR_NO_MATCH u tabletů.
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_400L)
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        1_800L,
                    )
                }
                // Upřednostnit síťové rozpoznávání (kde OEM nabízí), ne čistě offline.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra("android.speech.extra.PREFER_OFFLINE", false)
                }
            }
            recognizer.startListening(intent)
            cont.invokeOnCancellation {
                runCatching {
                    recognizer.stopListening()
                    recognizer.destroy()
                }
            }
        }

    private fun speechErrorLabel(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        else -> "UNKNOWN($error)"
    }
}
