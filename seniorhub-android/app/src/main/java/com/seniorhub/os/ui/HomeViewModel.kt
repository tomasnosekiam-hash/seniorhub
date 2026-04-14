package com.seniorhub.os.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.MvpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val device: DeviceSettings? = null,
    val deviceConfig: DeviceConfig? = null,
    val contacts: List<Contact> = emptyList(),
    val showPairingSheet: Boolean = false,
    val showKioskUnlock: Boolean = false,
    val kioskUnlockError: String? = null,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val repository: MvpRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var kioskSecretTapCount = 0
    private var kioskSecretTapAnchorMs = 0L

    init {
        viewModelScope.launch {
            runCatching { repository.bootstrapDevice() }
            combine(
                repository.observeDevice(),
                repository.observeContacts(),
                repository.observeDeviceConfig(),
            ) { deviceResult, contactsResult, configResult ->
                val deviceError = deviceResult.exceptionOrNull()
                val contactsError = contactsResult.exceptionOrNull()
                val configError = configResult.exceptionOrNull()
                val err = deviceError ?: contactsError ?: configError
                if (err != null) {
                    HomeUiState(
                        loading = false,
                        device = null,
                        deviceConfig = null,
                        contacts = emptyList(),
                        showPairingSheet = false,
                        showKioskUnlock = false,
                        kioskUnlockError = null,
                        errorMessage = err.message ?: err.toString(),
                    )
                } else {
                    val device = deviceResult.getOrNull()
                    HomeUiState(
                        loading = false,
                        device = device,
                        deviceConfig = configResult.getOrNull(),
                        contacts = contactsResult.getOrElse { emptyList() },
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
