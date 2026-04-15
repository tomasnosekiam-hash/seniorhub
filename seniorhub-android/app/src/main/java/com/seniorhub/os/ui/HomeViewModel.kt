package com.seniorhub.os.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.data.OpenMeteoWeather
import com.seniorhub.os.util.readActiveNetworkSummary
import com.seniorhub.os.util.readBatteryStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L
private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L

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

    private var kioskSecretTapCount = 0
    private var kioskSecretTapAnchorMs = 0L

    init {
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
            }
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
