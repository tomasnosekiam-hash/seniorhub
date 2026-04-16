package com.seniorhub.os.matej

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.MainActivity
import com.seniorhub.os.R
import com.seniorhub.os.SeniorHubApp
import java.io.File

/**
 * Naslouchání klíčovému slovu (Porcupine). Po detekci pozastaví engine a aplikace spustí TTS + STT.
 *
 * Výchozí klíčové slovo je vestavěné anglické „Porcupine“, pokud v [assets]/porcupine/keyword.ppn není vlastní .ppn
 * (viz [prepareCustomKeywordPath]). Pro češtinu „Matěj“ je potřeba vlastní soubor z Picovoice Console + [BuildConfig.PICOVOICE_ACCESS_KEY].
 */
class MatejForegroundService : Service() {

    private var porcupineManager: PorcupineManager? = null
    /** True pokud je v APK `assets/porcupine/keyword.ppn`; jinak vestavěné anglické „Porcupine“. */
    private var usingCustomKeywordFile = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) {
            Log.w(TAG, "PICOVOICE_ACCESS_KEY prázdný — přidej picovoice.access.key do local.properties (Picovoice access key).")
            return
        }
        try {
            val keywordPath = prepareCustomKeywordPath()
            usingCustomKeywordFile = keywordPath != null
            val builder = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setSensitivity(0.55f)
            if (keywordPath != null) {
                builder.setKeywordPath(keywordPath)
            } else {
                builder.setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
                Log.i(TAG, "Používám vestavěné klíčové slovo Porcupine — pro češtinu přidej assets/porcupine/keyword.ppn")
            }
            val callback = PorcupineManagerCallback { _: Int ->
                mainHandler.post {
                    Log.i(TAG, "Porcupine: detekováno klíčové slovo")
                    try {
                        porcupineManager?.stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "stop po wake", e)
                    }
                    (application as SeniorHubApp).emitMatejWake()
                }
            }
            porcupineManager = builder.build(applicationContext, callback)
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine init selhalo", e)
            porcupineManager = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListeningInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESUME_LISTENING -> {
                if (porcupineManager == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startListeningInternal()
                return START_STICKY
            }
            ACTION_START, null -> {
                if (porcupineManager == null) {
                    Log.w(TAG, "Porcupine není inicializovaný — služba se ukončí.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startListeningInternal()
                return START_STICKY
            }
            else -> return START_STICKY
        }
    }

    private fun startListeningInternal() {
        val mgr = porcupineManager ?: return
        try {
            mgr.start()
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine start selhalo", e)
        }
    }

    private fun stopListeningInternal() {
        val mgr = porcupineManager ?: return
        try {
            mgr.stop()
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine stop selhalo", e)
        }
    }

    override fun onDestroy() {
        try {
            porcupineManager?.stop()
        } catch (_: Exception) {
        }
        try {
            porcupineManager?.delete()
        } catch (_: Exception) {
        }
        porcupineManager = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (usingCustomKeywordFile) {
            getString(R.string.matej_wake_notification_text_custom_ppn)
        } else {
            getString(R.string.matej_wake_notification_text_builtin_porcupine)
        }
        return NotificationCompat.Builder(this, SeniorHubApp.CHANNEL_ID_MATEJ_WAKE)
            .setContentTitle(getString(R.string.matej_wake_notification_title))
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_simple)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun prepareCustomKeywordPath(): String? {
        return try {
            val out = File(filesDir, "porcupine_keyword.ppn")
            assets.open("porcupine/keyword.ppn").use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MatejFgService"
        private const val NOTIFICATION_ID = 71042
        const val ACTION_START = "com.seniorhub.os.matej.START"
        const val ACTION_STOP = "com.seniorhub.os.matej.STOP"
        const val ACTION_RESUME_LISTENING = "com.seniorhub.os.matej.RESUME"

        fun startWakeListening(context: Context) {
            if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, MatejForegroundService::class.java).setAction(ACTION_START),
            )
        }

        fun stopWakeListening(context: Context) {
            context.startService(
                Intent(context, MatejForegroundService::class.java).setAction(ACTION_STOP),
            )
        }

        /** Znovu spustí naslouchání po dokončení relace Matěje (TTS + STT). */
        fun resumeWakeListening(context: Context) {
            if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) return
            context.startService(
                Intent(context, MatejForegroundService::class.java).setAction(ACTION_RESUME_LISTENING),
            )
        }
    }
}
