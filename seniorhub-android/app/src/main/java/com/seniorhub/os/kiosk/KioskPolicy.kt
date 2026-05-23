package com.seniorhub.os.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Plný kiosk po `adb shell dpm set-device-owner com.seniorhub.os/.kiosk.KioskDeviceAdminReceiver`.
 * Bez device owner zůstává měkké připnutí přes [android.app.Activity.startLockTask].
 */
object KioskPolicy {
    private const val TAG = "KioskPolicy"

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, KioskDeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun enterDeviceOwnerKiosk(activity: Activity): Boolean {
        if (!isDeviceOwner(activity)) return false
        val dpm = activity.getSystemService(DevicePolicyManager::class.java) ?: return false
        val admin = adminComponent(activity)
        return runCatching {
            dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(admin, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE,
                )
            }
            activity.startLockTask()
            Log.i(TAG, "device-owner kiosk active")
            true
        }.onFailure { Log.e(TAG, "enterDeviceOwnerKiosk failed", it) }
            .getOrDefault(false)
    }
}
