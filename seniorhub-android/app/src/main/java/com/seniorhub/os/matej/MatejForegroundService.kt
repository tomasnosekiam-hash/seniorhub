package com.seniorhub.os.matej

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.R
import com.seniorhub.os.SeniorHubApp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.data.OpenMeteoWeather
import com.seniorhub.os.util.CellularSmsCapability
import com.seniorhub.os.util.normalizePhoneForDial
import com.seniorhub.os.util.SmsSender
import com.seniorhub.os.util.startOutgoingCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Modul Matěj (jen Senior): foreground service, wake word (Porcupine pokud je v `local.properties`
 * klíč `picovoice.access.key`, jinak STT), po probuzení krátký příkaz (počasí, vzkaz, SMS, hovor, nouze),
 * zápis `incidents` jen u nouzové větve + TTS.
 */
class MatejForegroundService : Service(), TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    private val snapshot = AtomicReference(MatejSnapshot())

    /** Po selhání inicializace Porcupine zůstane STT. */
    private var porcupineEnabled: Boolean = true

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        porcupineEnabled = BuildConfig.PICOVOICE_ACCESS_KEY.isNotBlank()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG && intent?.action == ACTION_TEST_EMERGENCY) {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, n)
            }
            mainHandler.post { runEmergencySequence() }
            return START_STICKY
        }
        if (intent?.action == ACTION_UPDATE) {
            applyUpdate(intent)
        }
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, n)
        }
        restartListening()
        return START_STICKY
    }

    private fun applyUpdate(intent: Intent) {
        porcupineEnabled = BuildConfig.PICOVOICE_ACCESS_KEY.isNotBlank()
        val assistant = intent.getStringExtra(EXTRA_ASSISTANT)?.trim().orEmpty()
        val seniorFirst = intent.getStringExtra(EXTRA_SENIOR_FIRST)?.trim().orEmpty()
        val fallbackSim = intent.getStringExtra(EXTRA_FALLBACK_SIM)?.trim().orEmpty()
        val lines = intent.getStringArrayListExtra(EXTRA_CONTACT_LINES).orEmpty()
        val contacts = lines.mapNotNull { line -> decodeContactLine(line) }
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)?.trim().orEmpty()
        snapshot.set(
            MatejSnapshot(
                deviceId = deviceId,
                assistantName = assistant.ifBlank { "Matěj" },
                seniorFirstName = seniorFirst,
                fallbackSim = fallbackSim,
                contacts = contacts,
            ),
        )
    }

    private fun restartListening() {
        listenJob?.cancel()
        listenJob = ioScope.launch {
            while (isActive) {
                val snap = snapshot.get()
                if (!snap.hasAssistant()) {
                    delay(2000)
                    continue
                }
                if (ContextCompat.checkSelfPermission(
                        this@MatejForegroundService,
                        android.Manifest.permission.RECORD_AUDIO,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    delay(4000)
                    continue
                }
                val heard = listenForWakeHybrid(snap.assistantName)
                if (heard) {
                    handlePostWake()
                    delay(WAKE_COOLDOWN_MS)
                } else {
                    delay(LISTEN_GAP_MS)
                }
            }
        }
    }

    private suspend fun listenForWakeHybrid(assistantName: String): Boolean {
        if (porcupineEnabled && BuildConfig.PICOVOICE_ACCESS_KEY.isNotBlank()) {
            val pv = awaitPorcupineWakeWord(this@MatejForegroundService)
            if (pv) return true
            porcupineEnabled = false
        }
        return listenForWakeOnce(assistantName)
    }

    private suspend fun listenForWakeOnce(assistantName: String): Boolean =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(this@MatejForegroundService)) return@withContext false
            suspendCancellableCoroutine { cont ->
                val recognizer = SpeechRecognizer.createSpeechRecognizer(this@MatejForegroundService)
                cont.invokeOnCancellation { recognizer.destroy() }
                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        recognizer.destroy()
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onResults(results: Bundle?) {
                        val text =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull().orEmpty()
                        recognizer.destroy()
                        if (cont.isActive) cont.resume(matchesAssistantWake(text, assistantName))
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val text =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull().orEmpty()
                        if (text.isNotBlank() && matchesAssistantWake(text, assistantName)) {
                            recognizer.stopListening()
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
                recognizer.setRecognitionListener(listener)
                recognizer.startListening(csRecognizeSpeechIntent(partialResults = true, prompt = null))
            }
        }

    /**
     * Jedno rozpoznání řeči (cs-CZ). [prompt] se předá do systému jako [RecognizerIntent.EXTRA_PROMPT] (např. diktát SMS).
     */
    private suspend fun listenForCommandOnce(prompt: String? = null): String =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(this@MatejForegroundService)) {
                return@withContext ""
            }
            suspendCancellableCoroutine { cont ->
                val recognizer = SpeechRecognizer.createSpeechRecognizer(this@MatejForegroundService)
                cont.invokeOnCancellation { recognizer.destroy() }
                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        recognizer.destroy()
                        if (cont.isActive) cont.resume("")
                    }

                    override fun onResults(results: Bundle?) {
                        val text =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull().orEmpty()
                        recognizer.destroy()
                        if (cont.isActive) cont.resume(text.trim())
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
                recognizer.setRecognitionListener(listener)
                recognizer.startListening(csRecognizeSpeechIntent(partialResults = false, prompt = prompt))
            }
        }

    private fun csRecognizeSpeechIntent(partialResults: Boolean, prompt: String?): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
            if (!prompt.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }

    private suspend fun handlePostWake() {
        val snap = snapshot.get()
        val transcript = listenForCommandOnce()
        when (val action = classifyPostWakeCommand(transcript, snap.contacts)) {
            MatejPostWakeAction.Weather -> runWeatherSkill()
            MatejPostWakeAction.ReadFamilyMessage -> runReadFamilyMessage()
            is MatejPostWakeAction.SendSms -> runSendSmsFlow(action.contact)
            MatejPostWakeAction.SmsNeedRecipient -> withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_sms_who), "matej_hint")
            }
            MatejPostWakeAction.DialNeedContact -> withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_dial_who), "matej_hint")
            }
            is MatejPostWakeAction.DialContact -> withContext(Dispatchers.Main) {
                runDialContact(action.contact)
            }
            MatejPostWakeAction.Emergency -> withContext(Dispatchers.Main) {
                runEmergencySequence()
            }
        }
    }

    private fun matejRepositoryOrNull(): MvpRepository? {
        val deviceId = snapshot.get().deviceId.trim()
        if (deviceId.isEmpty()) return null
        return MvpRepository(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance(),
            deviceId,
        )
    }

    private suspend fun runReadFamilyMessage() {
        val repo = matejRepositoryOrNull()
        if (repo == null) {
            withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_read_fail), "matej_read")
            }
            return
        }
        val msgResult = withContext(Dispatchers.IO) {
            runCatching { repo.fetchFirstUnreadFamilyMessage() }
        }
        if (msgResult.isFailure) {
            withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_read_fail), "matej_read")
            }
            return
        }
        val msg = msgResult.getOrNull()
        if (msg == null) {
            withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_no_unread), "matej_read")
            }
            return
        }
        val who = msg.senderDisplayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Rodina"
        withContext(Dispatchers.Main) {
            speakTts(getString(R.string.matej_tts_read_message, who, msg.body), "matej_read")
        }
        ioScope.launch {
            runCatching { repo.markMessageRead(msg.id) }
        }
    }

    private suspend fun runSendSmsFlow(contact: Contact) {
        val body = listenForCommandOnce(getString(R.string.matej_stt_prompt_sms_body))
        if (body.isBlank()) {
            withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_sms_no_body), "matej_sms")
            }
            return
        }
        val repo = matejRepositoryOrNull()
        if (repo == null) {
            withContext(Dispatchers.Main) {
                speakTts(getString(R.string.matej_tts_sms_fail), "matej_sms")
            }
            return
        }
        val ctx = applicationContext
        val useCell = CellularSmsCapability.canSendCellularSms(ctx)
        val hasSend =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
        val sendResult = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    useCell && hasSend -> {
                        SmsSender.send(ctx, contact.phone, body).getOrThrow()
                        repo.recordOutboundCellularSms(contact, body)
                    }
                    else -> repo.sendTabletFirestoreMessage(contact, body)
                }
            }
        }
        withContext(Dispatchers.Main) {
            sendResult.fold(
                onSuccess = {
                    speakTts(
                        getString(R.string.matej_tts_sms_ok, contact.name.trim()),
                        "matej_sms",
                    )
                },
                onFailure = {
                    speakTts(getString(R.string.matej_tts_sms_fail), "matej_sms")
                },
            )
        }
    }

    private suspend fun runWeatherSkill() {
        val line = withContext(Dispatchers.IO) {
            OpenMeteoWeather.fetchCurrentSummary().getOrNull()
        }
        val msg = line ?: getString(R.string.matej_tts_weather_error)
        withContext(Dispatchers.Main) {
            speakTts(msg, "matej_weather")
        }
    }

    private fun runDialContact(contact: Contact) {
        if (normalizePhoneForDial(contact.phone) == null) {
            runEmergencySequence()
            return
        }
        startOutgoingCall(contact.phone)
        mainHandler.postDelayed({
            speakTts(
                getString(R.string.matej_tts_dial_named, contact.name.trim()),
                "matej_dial",
            )
        }, 600L)
    }

    private fun runEmergencySequence() {
        val snap = snapshot.get()
        val target = pickEmergencyDialTarget(snap.contacts, snap.fallbackSim) ?: return
        val deviceId = snap.deviceId.trim()
        if (deviceId.isNotEmpty()) {
            ioScope.launch {
                runCatching {
                    val db = FirebaseFirestore.getInstance()
                    val auth = FirebaseAuth.getInstance()
                    MvpRepository(db, auth, deviceId).recordMatejEmergencyIncident(
                        dialedPhone = target.rawPhone,
                        dialedContactLabel = target.contactLabel,
                    )
                }
            }
        }
        startOutgoingCall(target.rawPhone)
        mainHandler.postDelayed({
            speakEmergencyTts(snap)
        }, TTS_AFTER_DIAL_DELAY_MS)
    }

    private fun speakEmergencyTts(snap: MatejSnapshot) {
        val first = snap.seniorFirstName.trim()
        val msg = if (first.isNotEmpty()) {
            getString(R.string.matej_tts_emergency_named, first)
        } else {
            getString(R.string.matej_tts_emergency_generic)
        }
        speakTts(msg, "matej_emergency")
    }

    private fun speakTts(message: String, utteranceId: String) {
        if (!ttsReady) return
        val engine = tts ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            engine.speak(message, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.forLanguageTag("cs-CZ")
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, SeniorHubApp.CHANNEL_ID_MATEJ)
            .setSmallIcon(R.drawable.ic_launcher_simple)
            .setContentTitle(getString(R.string.matej_notification_title))
            .setContentText(getString(R.string.matej_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        listenJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private data class MatejSnapshot(
        val deviceId: String = "",
        val assistantName: String = "Matěj",
        val seniorFirstName: String = "",
        val fallbackSim: String = "",
        val contacts: List<Contact> = emptyList(),
    ) {
        fun hasAssistant(): Boolean = assistantName.isNotBlank()
    }

    companion object {
        private const val NOTIFICATION_ID = 10042
        private const val LISTEN_GAP_MS = 1200L
        private const val WAKE_COOLDOWN_MS = 45_000L
        private const val TTS_AFTER_DIAL_DELAY_MS = 2200L

        const val ACTION_UPDATE = "com.seniorhub.os.matej.ACTION_UPDATE"
        /** Pouze debug build — spustí stejnou nouzovou sekvenci jako po probuzení (bez wake word). */
        const val ACTION_TEST_EMERGENCY = "com.seniorhub.os.matej.ACTION_TEST_EMERGENCY"

        /**
         * Debug: okamžitě provede nouzové vytočení + incident podle aktuálního snapshotu služby.
         * Vyžaduje běžící sync z dashboardu (spárované zařízení) a platný cíl (nouzový kontakt / SIM).
         */
        fun triggerTestEmergency(context: android.content.Context) {
            if (!BuildConfig.DEBUG) return
            val i = Intent(context, MatejForegroundService::class.java).apply {
                action = ACTION_TEST_EMERGENCY
            }
            ContextCompat.startForegroundService(context, i)
        }
        private const val EXTRA_ASSISTANT = "assistant"
        private const val EXTRA_SENIOR_FIRST = "senior_first"
        private const val EXTRA_FALLBACK_SIM = "fallback_sim"
        private const val EXTRA_CONTACT_LINES = "contact_lines"
        private const val EXTRA_DEVICE_ID = "device_id"

        fun sync(
            context: android.content.Context,
            config: DeviceConfig,
            contacts: List<Contact>,
            deviceId: String,
        ) {
            val lines = ArrayList(contacts.map { encodeContactLine(it) })
            val i = Intent(context, MatejForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_DEVICE_ID, deviceId)
                putExtra(EXTRA_ASSISTANT, config.assistantName)
                putExtra(EXTRA_SENIOR_FIRST, config.seniorFirstName)
                putExtra(EXTRA_FALLBACK_SIM, config.simNumber)
                putStringArrayListExtra(EXTRA_CONTACT_LINES, lines)
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, MatejForegroundService::class.java))
        }

        private fun encodeContactLine(c: Contact): String =
            "${c.id}\t${c.name.replace("\t", " ")}\t${c.phone}\t${c.isEmergency}"

        private fun decodeContactLine(line: String): Contact? {
            val parts = line.split("\t", limit = 4)
            if (parts.size < 4) return null
            val id = parts[0]
            val name = parts[1]
            val phone = parts[2]
            val emergency = parts[3] == "true"
            if (id.isBlank() && name.isBlank() && phone.isBlank()) return null
            return Contact(
                id = id,
                name = name,
                phone = phone,
                isEmergency = emergency,
                sortOrder = 0L,
            )
        }
    }
}
