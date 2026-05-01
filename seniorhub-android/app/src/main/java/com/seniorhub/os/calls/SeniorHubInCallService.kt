package com.seniorhub.os.calls

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class SeniorHubInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        SeniorHubCallManager.attachService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        SeniorHubCallManager.setCall(call)
        startActivity(
            Intent(this, SeniorHubCallOverlayActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        )
    }

    override fun onCallRemoved(call: Call) {
        SeniorHubCallManager.clearCall(call)
        super.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: android.telecom.CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        SeniorHubCallManager.updateState()
    }

    override fun onDestroy() {
        SeniorHubCallManager.detachService(this)
        super.onDestroy()
    }
}
