package com.seniorhub.os.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.seniorhub.os.data.AdminRepository
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.JoinedDeviceRow
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val authUser: FirebaseUser? = null,
    val loading: Boolean = true,
    val devices: List<JoinedDeviceRow> = emptyList(),
    val devicesError: String? = null,
    val selectedDeviceId: String? = null,
    val device: DeviceSettings? = null,
    val config: DeviceConfig? = null,
    val contacts: List<Contact> = emptyList(),
    val messages: List<DeviceMessage> = emptyList(),
    val detailError: String? = null,
    val pairCodeDraft: String = "",
    val pairError: String? = null,
    val banner: String? = null,
    val error: String? = null,
)

class AdminViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val repository: AdminRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private val selectedDeviceId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            authStateFlow().collect { user ->
                if (user != null && !user.isAnonymous) {
                    runCatching {
                        val token = FirebaseMessaging.getInstance().token.await()
                        repository.registerAdminFcmToken(token)
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                authStateFlow(),
                selectedDeviceId,
            ) { user, sel ->
                user to sel
            }
                .flatMapLatest { (user, sel) ->
                    if (user == null || sel.isNullOrBlank()) {
                        flowOf(
                            AdminDeviceDetailBundle(
                                device = Result.success(null),
                                config = Result.success(null),
                                messages = Result.success(emptyList()),
                                contacts = Result.success(emptyList()),
                            ),
                        )
                    } else {
                        combine(
                            repository.observeDevice(sel),
                            repository.observeDeviceConfig(sel),
                            repository.observeMessages(sel),
                            repository.observeContacts(sel),
                        ) { d, c, m, cont ->
                            AdminDeviceDetailBundle(d, c, m, cont)
                        }
                    }
                }
                .collect { bundle ->
                    val err = bundle.device.exceptionOrNull()
                        ?: bundle.config.exceptionOrNull()
                        ?: bundle.messages.exceptionOrNull()
                        ?: bundle.contacts.exceptionOrNull()
                    _state.update {
                        it.copy(
                            device = bundle.device.getOrNull(),
                            config = bundle.config.getOrNull(),
                            messages = bundle.messages.getOrElse { emptyList() },
                            contacts = bundle.contacts.getOrElse { emptyList() },
                            detailError = err?.message ?: err?.toString(),
                        )
                    }
                }
        }

        viewModelScope.launch {
            authStateFlow()
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    repository.observeJoinedDevices(uid)
                }
                .collect { devicesResult ->
                val err = devicesResult.exceptionOrNull()
                val list = devicesResult.getOrElse { emptyList() }
                val user = auth.currentUser
                _state.update {
                    it.copy(
                        authUser = user,
                        loading = false,
                        devices = list,
                        devicesError = err?.message ?: err?.toString(),
                    )
                }
                val currentSel = selectedDeviceId.value
                if (list.isEmpty()) {
                    if (currentSel != null) {
                        selectedDeviceId.value = null
                        _state.update { s -> s.copy(selectedDeviceId = null) }
                    }
                } else if (currentSel == null || list.none { it.deviceId == currentSel }) {
                    val pick = list.first().deviceId
                    selectedDeviceId.value = pick
                    _state.update { s -> s.copy(selectedDeviceId = pick) }
                }
            }
        }
    }

    fun selectDevice(deviceId: String) {
        selectedDeviceId.value = deviceId
        _state.update { it.copy(selectedDeviceId = deviceId, banner = null, error = null) }
    }

    fun setPairDraft(value: String) {
        _state.update { it.copy(pairCodeDraft = value.uppercase(), pairError = null) }
    }

    fun pair() {
        val code = _state.value.pairCodeDraft.trim()
        if (code.length < 4) {
            _state.update { it.copy(pairError = "Zadej kód z tabletu.") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.pairWithCode(code) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            pairCodeDraft = "",
                            pairError = null,
                            banner = "Tablet spárován.",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(pairError = e.message ?: e.toString()) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { repository.signOut() }
            selectedDeviceId.value = null
            _state.value = AdminUiState(loading = false)
        }
    }

    fun onGoogleSignInSuccess(idToken: String) {
        if (idToken.isBlank()) {
            _state.update {
                it.copy(
                    error = "Google nepředal token. Ověř Web client ID v strings.xml a SHA-1/256 v Firebase Console.",
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching { repository.signInWithGoogleIdToken(idToken) }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: e.toString()) }
                }
                .onSuccess {
                    _state.update { it.copy(error = null) }
                }
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun dismissBanner() {
        _state.update { it.copy(banner = null) }
    }

    fun saveDeviceMeta(deviceLabel: String, volumePercent: Int, alertMessage: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.saveDeviceMeta(
                    deviceId = id,
                    deviceLabel = deviceLabel,
                    volumePercent = volumePercent,
                    alertMessage = alertMessage,
                )
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: e.toString()) }
            }.onSuccess {
                _state.update { it.copy(banner = "Nastavení uloženo.", error = null) }
            }
        }
    }

    fun clearAlert() {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching { repository.clearAlert(id) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
                .onSuccess { _state.update { it.copy(banner = "Hláška vymazána.", error = null) } }
        }
    }

    fun saveConfig(adminPin: String, simNumber: String, assistantName: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.saveConfigBlock(
                    deviceId = id,
                    adminPin = adminPin,
                    simNumber = simNumber,
                    assistantName = assistantName,
                )
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: e.toString()) }
            }.onSuccess {
                _state.update { it.copy(banner = "PIN a provoz uloženy.", error = null) }
            }
        }
    }

    fun saveSeniorProfile(first: String, last: String, address: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.saveSeniorProfile(
                    deviceId = id,
                    seniorFirstName = first,
                    seniorLastName = last,
                    addressLine = address,
                )
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: e.toString()) }
            }.onSuccess {
                _state.update { it.copy(banner = "Profil seniora uložen.", error = null) }
            }
        }
    }

    fun sendMessage(body: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching { repository.sendMessage(id, body) }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: e.toString()) }
                }
                .onSuccess {
                    _state.update { it.copy(banner = "Vzkaz odeslán.", error = null) }
                }
        }
    }

    fun addContact(name: String, phone: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching { repository.addContact(id, name, phone) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
                .onSuccess { _state.update { it.copy(banner = "Kontakt přidán.", error = null) } }
        }
    }

    fun setContactEmergency(contactId: String, isEmergency: Boolean) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching { repository.setContactEmergency(id, contactId, isEmergency) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
        }
    }

    fun deleteContact(contactId: String) {
        val id = selectedDeviceId.value ?: return
        viewModelScope.launch {
            runCatching { repository.deleteContact(id, contactId) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
                .onSuccess { _state.update { it.copy(banner = "Kontakt smazán.", error = null) } }
        }
    }

    fun moveContactUp(contactId: String) {
        val deviceId = selectedDeviceId.value ?: return
        val list = _state.value.contacts
        val idx = list.indexOfFirst { it.id == contactId }
        if (idx <= 0) return
        val otherId = list[idx - 1].id
        viewModelScope.launch {
            runCatching { repository.swapContactSortOrders(deviceId, contactId, otherId) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
        }
    }

    fun moveContactDown(contactId: String) {
        val deviceId = selectedDeviceId.value ?: return
        val list = _state.value.contacts
        val idx = list.indexOfFirst { it.id == contactId }
        if (idx < 0 || idx >= list.lastIndex) return
        val otherId = list[idx + 1].id
        viewModelScope.launch {
            runCatching { repository.swapContactSortOrders(deviceId, contactId, otherId) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: e.toString()) } }
        }
    }

    private fun authStateFlow() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}

private data class AdminDeviceDetailBundle(
    val device: Result<DeviceSettings?>,
    val config: Result<DeviceConfig?>,
    val messages: Result<List<DeviceMessage>>,
    val contacts: Result<List<Contact>>,
)
