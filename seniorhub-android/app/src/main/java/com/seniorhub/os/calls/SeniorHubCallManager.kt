package com.seniorhub.os.calls

import android.telecom.Call
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SeniorHubCallUiState(
    val active: Boolean = false,
    val displayName: String = "",
    val stateLabel: String = "",
    val speakerOn: Boolean = false,
)

object SeniorHubCallManager {
    private val _state = MutableStateFlow(SeniorHubCallUiState())
    val state: StateFlow<SeniorHubCallUiState> = _state.asStateFlow()

    private var service: SeniorHubInCallService? = null
    private var currentCall: Call? = null
    private var callback: Call.Callback? = null

    fun attachService(next: SeniorHubInCallService) {
        service = next
        updateState()
    }

    fun detachService(detached: SeniorHubInCallService) {
        if (service == detached) service = null
        updateState()
    }

    fun setCall(call: Call?) {
        callback?.let { cb -> currentCall?.unregisterCallback(cb) }
        currentCall = call
        callback = call?.let {
            object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    updateState()
                }

                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateState()
                }
            }.also { cb -> it.registerCallback(cb) }
        }
        updateState()
    }

    fun clearCall(call: Call) {
        if (currentCall == call) {
            setCall(null)
        }
    }

    fun disconnect() {
        currentCall?.disconnect()
    }

    fun setSpeaker(enabled: Boolean) {
        service?.setAudioRoute(
            if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
        )
        updateState()
    }

    fun updateState() {
        val call = currentCall
        val audio = service?.callAudioState
        _state.value = SeniorHubCallUiState(
            active = call != null,
            displayName = call?.details?.handle?.schemeSpecificPart.orEmpty(),
            stateLabel = call?.state?.toCallStateLabel().orEmpty(),
            speakerOn = audio?.route == CallAudioState.ROUTE_SPEAKER,
        )
    }

    private fun Int.toCallStateLabel(): String = when (this) {
        Call.STATE_ACTIVE -> "Probíhá hovor"
        Call.STATE_CONNECTING -> "Vytáčím"
        Call.STATE_DIALING -> "Vytáčím"
        Call.STATE_HOLDING -> "Podrženo"
        Call.STATE_RINGING -> "Vyzvání"
        Call.STATE_DISCONNECTED -> "Ukončeno"
        Call.STATE_DISCONNECTING -> "Ukončuji"
        else -> "Hovor"
    }
}
