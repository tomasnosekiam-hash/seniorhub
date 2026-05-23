package com.seniorhub.os.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.seniorhub.os.kiosk.KioskPolicy

/**
 * Kiosk na běžném tabletu: [Activity.startLockTask] + manifest HOME intent (výchozí launcher).
 * Plný režim bez lišty: Device Owner — viz [com.seniorhub.os.kiosk.KioskPolicy] a `docs/KIOSK_TABLET_SETUP.md`.
 */
object KioskMode {
    private const val TAG = "KioskMode"

    fun prepareWakeToApp(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    fun clearWakeToApp(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(false)
            activity.setTurnScreenOn(false)
        }
        @Suppress("DEPRECATION")
        activity.window.clearFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
    }

    fun isInLockTask(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val am = activity.getSystemService(ActivityManager::class.java) ?: return false
        return when (am.lockTaskModeState) {
            ActivityManager.LOCK_TASK_MODE_LOCKED,
            ActivityManager.LOCK_TASK_MODE_PINNED -> true
            else -> false
        }
    }

    /** Po klepnutí na „Připnout“ — true když režim opravdu běží. */
    fun tryStartPinning(activity: Activity, onResult: ((Boolean) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onResult?.invoke(false)
            return
        }
        if (isInLockTask(activity)) {
            onResult?.invoke(true)
            return
        }
        prepareWakeToApp(activity)
        if (KioskPolicy.enterDeviceOwnerKiosk(activity)) {
            onResult?.invoke(true)
            return
        }

        runCatching { activity.startLockTask() }
            .onSuccess {
                val state = lockTaskState(activity)
                Log.i(TAG, "startLockTask returned, lockTaskModeState=$state")
                Handler(Looper.getMainLooper()).postDelayed({
                    val pinned = isInLockTask(activity)
                    if (!pinned) {
                        Log.w(TAG, "Screen pinning dialog missing — likely disabled in system settings")
                    }
                    onResult?.invoke(pinned)
                }, 900)
            }
            .onFailure { e ->
                Log.e(TAG, "startLockTask failed", e)
                Toast.makeText(
                    activity,
                    "Kiosk: ${e.message ?: "startLockTask selhalo"}",
                    Toast.LENGTH_LONG,
                ).show()
                onResult?.invoke(false)
            }
    }

    /** Nastavení → Zabezpečení (tam bývá „Připnutí obrazovky“ / Screen pinning). */
    fun openPinningSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        }
        Toast.makeText(context, "Otevřete Nastavení → Zabezpečení ručně.", Toast.LENGTH_LONG).show()
    }

    fun tryStopPinning(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        if (!isInLockTask(activity)) return
        runCatching { activity.stopLockTask() }
            .onFailure { Log.w(TAG, "stopLockTask failed", it) }
    }

    private fun lockTaskState(activity: Activity): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return -1
        val am = activity.getSystemService(ActivityManager::class.java) ?: return -1
        return am.lockTaskModeState
    }

    fun isOurPackageDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val pkg = resolve?.activityInfo?.packageName
        return pkg != null && pkg == context.packageName
    }
}
