package com.seniorhub.os.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.R
import com.seniorhub.os.SeniorHubApp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.data.OpenMeteoWeather
import com.seniorhub.os.matej.MatejBrainFactory
import com.seniorhub.os.matej.MatejBrainInput
import com.seniorhub.os.matej.MatejConversationTurn
import com.seniorhub.os.matej.MatejConfirmationPhrases
import com.seniorhub.os.matej.MatejForegroundService
import com.seniorhub.os.matej.MatejTurnOutcome
import com.seniorhub.os.matej.MatejVoicePipeline
import com.seniorhub.os.util.CellularSmsCapability
import com.seniorhub.os.util.SmsSender
import com.seniorhub.os.util.normalizePhoneForDial
import com.seniorhub.os.util.openDialPad
import com.seniorhub.os.util.readActiveNetworkSummary
import com.seniorhub.os.util.readBatteryStatus
import com.seniorhub.os.util.startOutgoingCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.coroutines.coroutineContext

private const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L
private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L
/** Po jakémkoli TTS — než otevřeme mikrofon (stabilnější než okamžitý start STT). */
private const val MATEJ_AFTER_TTS_BEFORE_LISTEN_MS = 450L
/** Mezi hlavní odpovědí a krátkou nápovědou „poslouchám“ po otázce. */
private const val MATEJ_BEFORE_LISTEN_NUDGE_MS = 160L
/** Ochrana proti nekonečné smyčce při chybě (uživatel může kdykoli zavřít overlay). */
private const val MATEJ_MAX_SESSION_TURNS = 48

data class HomeUiState(
    val loading: Boolean = true,
    val device: DeviceSettings? = null,
    val deviceConfig: DeviceConfig? = null,
    val contacts: List<Contact> = emptyList(),
    /** Nejnovější nepřečtený vzkaz od rodiny (ne odchozí z tabletu). */
    val unreadMessage: DeviceMessage? = null,
    /** Všechny zprávy z Firestore (sestupně podle `createdAt`). */
    val messages: List<DeviceMessage> = emptyList(),
    val weatherLine: String? = null,
    val showPairingSheet: Boolean = false,
    val showKioskUnlock: Boolean = false,
    val kioskUnlockError: String? = null,
    val errorMessage: String? = null,
)

class HomeViewModel(
    application: Application,
    private val repository: MvpRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _matejSession = MutableStateFlow<MatejUiSession?>(null)
    val matejSession: StateFlow<MatejUiSession?> = _matejSession.asStateFlow()

    private var matejVoiceJob: Job? = null

    private val matejBrain by lazy { MatejBrainFactory.create(getApplication()) }

    private var kioskSecretTapCount = 0
    private var kioskSecretTapAnchorMs = 0L

    /** Poslední známý stav „můžeme spustit Porcupine FGS“ — aby se při každé zprávě z Firestore nevolal start opakovaně. */
    private var lastMatejWakeEligible: Boolean? = null
    private var loggedMissingPicovoiceKey = false

    init {
        viewModelScope.launch {
            (getApplication() as SeniorHubApp).matejWakeSignals.collect {
                onMatejWakeFromKeyword()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val status = readBatteryStatus(getApplication())
                    val (netType, netLabel) = readActiveNetworkSummary(getApplication())
                    repository.postDeviceHeartbeat(
                        status.percent,
                        status.charging,
                        networkType = netType,
                        networkLabel = netLabel,
                    )
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            runCatching {
                OpenMeteoWeather.fetchCurrentSummary().getOrNull()?.let { line ->
                    _state.update { it.copy(weatherLine = line) }
                }
            }
            while (isActive) {
                delay(WEATHER_REFRESH_MS)
                runCatching {
                    OpenMeteoWeather.fetchCurrentSummary().getOrNull()?.let { line ->
                        _state.update { it.copy(weatherLine = line) }
                    }
                }
            }
        }
        viewModelScope.launch {
            runCatching { repository.bootstrapDevice() }
            combine(
                repository.observeDevice(),
                repository.observeContacts(),
                repository.observeDeviceConfig(),
                repository.observeMessages(),
            ) { deviceResult, contactsResult, configResult, messagesResult ->
                val deviceError = deviceResult.exceptionOrNull()
                val contactsError = contactsResult.exceptionOrNull()
                val configError = configResult.exceptionOrNull()
                val messagesError = messagesResult.exceptionOrNull()
                val err = deviceError ?: contactsError ?: configError ?: messagesError
                if (err != null) {
                    HomeUiState(
                        loading = false,
                        device = null,
                        deviceConfig = null,
                        contacts = emptyList(),
                        unreadMessage = null,
                        messages = emptyList(),
                        weatherLine = null,
                        showPairingSheet = false,
                        showKioskUnlock = false,
                        kioskUnlockError = null,
                        errorMessage = err.message ?: err.toString(),
                    )
                } else {
                    val device = deviceResult.getOrNull()
                    val messages = messagesResult.getOrElse { emptyList() }
                    // Jen vzkazy z webu (bez `delivery`) — ne odchozí z tabletu ani příchozí SMS.
                    val unread = messages.firstOrNull {
                        it.readAt == null &&
                            it.body.isNotBlank() &&
                            it.delivery.isNullOrBlank()
                    }
                    HomeUiState(
                        loading = false,
                        device = device,
                        deviceConfig = configResult.getOrNull(),
                        contacts = contactsResult.getOrElse { emptyList() },
                        unreadMessage = unread,
                        messages = messages,
                        weatherLine = _state.value.weatherLine,
                        showPairingSheet = device?.paired != true,
                        showKioskUnlock = false,
                        kioskUnlockError = null,
                        errorMessage = null,
                    )
                }
            }.collect { next ->
                val current = _state.value
                val mustShowPairing = next.device?.paired != true
                _state.value = next.copy(
                    showPairingSheet = mustShowPairing || current.showPairingSheet,
                    showKioskUnlock = current.showKioskUnlock,
                    kioskUnlockError = current.kioskUnlockError,
                )
                syncMatejWakeService()
            }
        }
    }

    /**
     * Spustí / zastaví foreground naslouchání klíčovému slovu (Porcupine), pokud je tablet spárovaný
     * a je uděleno [android.Manifest.permission.RECORD_AUDIO] a je nastaven Picovoice klíč.
     *
     * @param force při true znovu zkusí start (např. po [ON_RESUME], když FGS spadla se stejnými podmínkami).
     */
    fun syncMatejWakeService(force: Boolean = false) {
        val app = getApplication<Application>()
        val paired = _state.value.device?.paired == true
        val mic = app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val keyOk = BuildConfig.PICOVOICE_ACCESS_KEY.isNotBlank()
        val eligible = paired && mic && keyOk
        if (paired && mic && !keyOk && !loggedMissingPicovoiceKey) {
            loggedMissingPicovoiceKey = true
            Log.w(
                "HomeViewModel",
                "Matěj wake: v APK chybí Picovoice klíč — přidej do seniorhub-android/local.properties řádek " +
                    "picovoice.access.key=… a znovu sestav a nainstaluj APK (bez něj se FGS vůbec nespustí).",
            )
        }
        if (!force && eligible == lastMatejWakeEligible) return
        lastMatejWakeEligible = eligible

        if (eligible) {
            MatejForegroundService.startWakeListening(app)
        } else {
            MatejForegroundService.stopWakeListening(app)
        }
    }

    private fun onMatejWakeFromKeyword() {
        if (matejVoiceJob?.isActive == true) return
        matejVoiceJob = viewModelScope.launch {
            runMatejAssistantSession(compactUi = false, resumeWakeAfter = true)
        }
    }

    /**
     * Ruční start relace Matěje 2.0 (dashboard) — probuzení z klíčového slova volá stejný tok.
     */
    fun startMatejAssistant() {
        if (matejVoiceJob?.isActive == true) return
        if (_state.value.device?.paired != true) return
        val app = getApplication<Application>()
        if (app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        matejVoiceJob = viewModelScope.launch {
            runMatejAssistantSession(compactUi = false, resumeWakeAfter = false)
        }
    }

    private suspend fun runMatejAssistantSession(compactUi: Boolean, resumeWakeAfter: Boolean) {
        val app = getApplication<Application>()
        val history = mutableListOf<MatejConversationTurn>()
        try {
            _matejSession.value = MatejUiSession(MatejPhase.Greeting, compactUi)
            MatejVoicePipeline.speakGreeting(app)
            if (!coroutineContext.isActive) return
            delay(MATEJ_AFTER_TTS_BEFORE_LISTEN_MS)
            if (!coroutineContext.isActive) return

            var turns = 0
            while (coroutineContext.isActive && turns < MATEJ_MAX_SESSION_TURNS) {
                turns++
                _matejSession.value = MatejUiSession(MatejPhase.Listening, compactUi)
                val utterance = MatejVoicePipeline.listenOnceCsOrTimeout(app)
                if (!coroutineContext.isActive) return
                val userLine = utterance?.trim().orEmpty()

                if (MatejConfirmationPhrases.looksLikeSessionEnd(utterance)) {
                    val bye = app.getString(R.string.matej_session_goodbye)
                    _matejSession.value = MatejUiSession(MatejPhase.Processing, compactUi)
                    MatejVoicePipeline.speakText(app, bye)
                    break
                }

                _matejSession.value = MatejUiSession(MatejPhase.Processing, compactUi)
                val brainInput = MatejBrainInput(
                    context = app,
                    utterance = utterance,
                    weatherLine = _state.value.weatherLine,
                    now = LocalTime.now(),
                    contacts = _state.value.contacts,
                    conversationHistory = history.toList(),
                )
                val outcome = matejBrain.decide(brainInput)
                if (!coroutineContext.isActive) return

                when (outcome) {
                    is MatejTurnOutcome.Speak -> {
                        val main = outcome.text
                        MatejVoicePipeline.speakText(app, main)
                        var assistantForHistory = main
                        if (main.trimEnd().endsWith('?')) {
                            delay(MATEJ_BEFORE_LISTEN_NUDGE_MS)
                            if (!coroutineContext.isActive) return
                            val nudge = app.getString(R.string.matej_listen_after_question)
                            MatejVoicePipeline.speakText(app, nudge)
                            assistantForHistory = "$main $nudge"
                        }
                        history.add(MatejConversationTurn(userText = userLine, assistantText = assistantForHistory))
                    }
                    is MatejTurnOutcome.ConfirmSendSms -> {
                        val summary = runConfirmSendSms(app, outcome, compactUi)
                        history.add(MatejConversationTurn(userText = userLine, assistantText = summary))
                    }
                    is MatejTurnOutcome.ConfirmCall -> {
                        val summary = runConfirmCall(app, outcome, compactUi)
                        history.add(MatejConversationTurn(userText = userLine, assistantText = summary))
                    }
                }
                if (!coroutineContext.isActive) return
                delay(MATEJ_AFTER_TTS_BEFORE_LISTEN_MS)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Matej relace", e)
        } finally {
            _matejSession.value = null
            matejVoiceJob = null
            if (resumeWakeAfter) {
                MatejForegroundService.resumeWakeListening(app)
            }
        }
    }

    /** @return text poslední zpětné vazby pro historii konverzace. */
    private suspend fun runConfirmSendSms(
        app: Application,
        pending: MatejTurnOutcome.ConfirmSendSms,
        compactUi: Boolean,
    ): String {
        _matejSession.value = MatejUiSession(MatejPhase.Confirming, compactUi)
        MatejVoicePipeline.speakText(app, pending.promptSpoken)
        if (!coroutineContext.isActive) throw CancellationException()
        delay(MATEJ_AFTER_TTS_BEFORE_LISTEN_MS)
        if (!coroutineContext.isActive) throw CancellationException()
        val answer = MatejVoicePipeline.listenOnceCsOrTimeout(app)
        if (!coroutineContext.isActive) throw CancellationException()
        _matejSession.value = MatejUiSession(MatejPhase.Processing, compactUi)
        return when {
            MatejConfirmationPhrases.isAffirmative(answer) -> {
                val msg = executeSendSmsAfterConfirm(app, pending.contact, pending.body)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
            MatejConfirmationPhrases.isNegative(answer) -> {
                val msg = app.getString(R.string.matej_confirm_cancelled)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
            else -> {
                val msg = app.getString(R.string.matej_confirm_unclear)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
        }
    }

    /** @return text poslední zpětné vazby pro historii konverzace. */
    private suspend fun runConfirmCall(
        app: Application,
        pending: MatejTurnOutcome.ConfirmCall,
        compactUi: Boolean,
    ): String {
        _matejSession.value = MatejUiSession(MatejPhase.Confirming, compactUi)
        MatejVoicePipeline.speakText(app, pending.promptSpoken)
        if (!coroutineContext.isActive) throw CancellationException()
        delay(MATEJ_AFTER_TTS_BEFORE_LISTEN_MS)
        if (!coroutineContext.isActive) throw CancellationException()
        val answer = MatejVoicePipeline.listenOnceCsOrTimeout(app)
        if (!coroutineContext.isActive) throw CancellationException()
        _matejSession.value = MatejUiSession(MatejPhase.Processing, compactUi)
        return when {
            MatejConfirmationPhrases.isAffirmative(answer) -> {
                val msg = executeCallAfterConfirm(app, pending.contact)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
            MatejConfirmationPhrases.isNegative(answer) -> {
                val msg = app.getString(R.string.matej_confirm_cancelled)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
            else -> {
                val msg = app.getString(R.string.matej_confirm_unclear)
                MatejVoicePipeline.speakText(app, msg)
                msg
            }
        }
    }

    private suspend fun executeSendSmsAfterConfirm(app: Application, contact: Contact, body: String): String {
        if (normalizePhoneForDial(contact.phone) == null) {
            return app.getString(R.string.matej_brain_invalid_phone)
        }
        if (!CellularSmsCapability.canSendCellularSms(app)) {
            return runCatching {
                repository.sendTabletFirestoreMessage(contact, body)
                app.getString(R.string.matej_sms_sent_firestore)
            }.getOrElse { e ->
                e.message ?: app.getString(R.string.matej_sms_send_failed)
            }
        }
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return app.getString(R.string.matej_sms_permission_denied)
        }
        return SmsSender.send(app, contact.phone, body).fold(
            onSuccess = {
                runCatching { repository.recordOutboundCellularSms(contact, body) }
                    .fold(
                        onSuccess = { app.getString(R.string.matej_sms_sent_ok) },
                        onFailure = {
                            app.getString(R.string.matej_sms_sent_cellular_cloud_failed)
                        },
                    )
            },
            onFailure = { e -> e.message ?: app.getString(R.string.matej_sms_send_failed) },
        )
    }

    private fun executeCallAfterConfirm(app: Application, contact: Contact): String {
        val phone = contact.phone
        if (normalizePhoneForDial(phone) == null) {
            return app.getString(R.string.matej_brain_invalid_phone)
        }
        return if (ContextCompat.checkSelfPermission(app, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            if (app.startOutgoingCall(phone)) {
                app.getString(R.string.matej_call_started)
            } else {
                app.getString(R.string.matej_call_failed)
            }
        } else {
            app.openDialPad(phone)
            app.getString(R.string.matej_call_dial_pad)
        }
    }

    fun dismissMatej() {
        matejVoiceJob?.cancel()
    }

    /** Jen [BuildConfig.DEBUG] — ruční náhled UI bez Porcupine. */
    fun cycleMatejDebugUiState() {
        if (!BuildConfig.DEBUG) return
        _matejSession.value = cycleMatejDebugStateImpl(_matejSession.value)
    }

    private fun cycleMatejDebugStateImpl(current: MatejUiSession?): MatejUiSession? {
        return when {
            current == null -> MatejUiSession(MatejPhase.Greeting, compact = false)
            current.phase == MatejPhase.Greeting ->
                MatejUiSession(MatejPhase.Listening, compact = false)
            current.phase == MatejPhase.Listening && !current.compact ->
                MatejUiSession(MatejPhase.Listening, compact = true)
            current.phase == MatejPhase.Listening && current.compact ->
                MatejUiSession(MatejPhase.Processing, compact = true)
            current.phase == MatejPhase.Processing ->
                MatejUiSession(MatejPhase.Confirming, compact = current.compact)
            current.phase == MatejPhase.Confirming -> null
            else -> null
        }
    }

    fun dismissAlert() {
        viewModelScope.launch {
            runCatching { repository.dismissAlert() }
        }
    }

    fun dismissUnreadMessage() {
        val id = _state.value.unreadMessage?.id ?: return
        viewModelScope.launch {
            runCatching { repository.markMessageRead(id) }
        }
    }

    /**
     * Tablet bez mobilní SMS — záznam do Firestore (rodina ve webu); viz [com.seniorhub.os.data.MvpRepository.sendTabletFirestoreMessage].
     */
    fun sendTabletFirestoreMessage(contact: Contact, body: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { repository.sendTabletFirestoreMessage(contact, body) })
        }
    }

    fun recordOutboundCellularSms(contact: Contact, body: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { repository.recordOutboundCellularSms(contact, body) })
        }
    }

    fun showPairingSheet() {
        _state.value = _state.value.copy(showPairingSheet = true)
    }

    fun hidePairingSheet() {
        if (_state.value.device?.paired == true) {
            _state.value = _state.value.copy(showPairingSheet = false)
        }
    }

    fun refreshPairingCode(force: Boolean = true) {
        viewModelScope.launch {
            val result = runCatching { repository.rotatePairingCodeIfNeeded(force = force) }
            result.exceptionOrNull()?.let { error ->
                _state.value = _state.value.copy(
                    errorMessage = error.message ?: error.toString(),
                )
            }
        }
    }

    /** Pět rychlých klepnutí do skryté zóny otevře zadání PINu (kiosk break). */
    fun onKioskSecretTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - kioskSecretTapAnchorMs > 1500L) {
            kioskSecretTapCount = 0
        }
        kioskSecretTapCount++
        kioskSecretTapAnchorMs = now
        if (kioskSecretTapCount >= 5) {
            kioskSecretTapCount = 0
            _state.value = _state.value.copy(
                showKioskUnlock = true,
                kioskUnlockError = null,
            )
        }
    }

    fun dismissKioskUnlock() {
        _state.value = _state.value.copy(
            showKioskUnlock = false,
            kioskUnlockError = null,
        )
    }

    /** @return true při shodě s PINem ve Firebase — volající může otevřít systémová nastavení. */
    fun tryUnlockWithPin(entered: String): Boolean {
        val normalized = entered.filter { it.isDigit() }.take(4)
        val expected = _state.value.deviceConfig?.adminPin?.trim().orEmpty()
        if (expected.length != 4 || !expected.all { it.isDigit() }) {
            _state.value = _state.value.copy(kioskUnlockError = "PIN zatím není platný v cloudu.")
            return false
        }
        if (normalized != expected) {
            _state.value = _state.value.copy(kioskUnlockError = "Nesprávný PIN.")
            return false
        }
        _state.value = _state.value.copy(
            showKioskUnlock = false,
            kioskUnlockError = null,
        )
        return true
    }
}
