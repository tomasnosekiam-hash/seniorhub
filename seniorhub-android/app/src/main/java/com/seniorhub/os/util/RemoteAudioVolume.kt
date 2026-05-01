package com.seniorhub.os.util

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlin.math.roundToInt

/**
 * Applies per-device remote volume from Firestore to the Android streams that SeniorHub uses for
 * family messages, system alerts, SMS feedback, TTS/media and calls.
 */
object RemoteAudioVolume {
    private const val TAG = "RemoteAudioVolume"
    private const val PREFS = "seniorhub_audio"
    private const val KEY_LAST_PERCENT = "last_volume_percent"

    fun apply(context: Context, rawPercent: Int) {
        val percent = rawPercent.coerceIn(0, 100)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_PERCENT, percent)
            .apply()
        applyPercent(context, percent)
    }

    fun applyLastKnown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAST_PERCENT)) return
        applyPercent(context, prefs.getInt(KEY_LAST_PERCENT, 50).coerceIn(0, 100))
    }

    private fun applyPercent(context: Context, percent: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val streams = intArrayOf(
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_VOICE_CALL,
        )
        for (stream in streams) {
            val max = audio.getStreamMaxVolume(stream)
            if (max <= 0) continue
            val target = (max * (percent / 100f)).roundToInt().coerceIn(0, max)
            try {
                audio.setStreamVolume(stream, target, 0)
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot set stream volume $stream", e)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Cannot set stream volume $stream", e)
            }
        }
    }
}
