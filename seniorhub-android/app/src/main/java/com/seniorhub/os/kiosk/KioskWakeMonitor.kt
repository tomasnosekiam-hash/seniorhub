package com.seniorhub.os.kiosk

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.seniorhub.os.util.KioskMode

/**
 * Po probuzení displeje znovu zapne lock task a přivede uživatele do SeniorHubu
 * (bez nutnosti opouštět systém přes notifikace třetích stran).
 */
class KioskWakeMonitor(
    private val activity: Activity,
    private val shouldPin: () -> Boolean,
) {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    if (shouldPin()) {
                        KioskMode.prepareWakeToApp(activity)
                        KioskMode.tryStartPinning(activity)
                    }
                }
            }
        }
    }

    private var registered = false

    fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            activity.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { activity.unregisterReceiver(receiver) }
        registered = false
    }
}
