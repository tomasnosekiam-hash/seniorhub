package com.seniorhub.os.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deviceIdentityDataStore by preferencesDataStore(name = "device_identity")

class DeviceIdentityStore(
    private val context: Context,
) {

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.deviceIdentityDataStore.data.first()[DEVICE_ID_KEY]
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty().ifBlank { "unknown" }
        val deviceId = "tablet-$androidId"
        context.deviceIdentityDataStore.edit { prefs ->
            prefs[DEVICE_ID_KEY] = deviceId
        }
        return deviceId
    }

    private companion object {
        val DEVICE_ID_KEY: Preferences.Key<String> = stringPreferencesKey("device_id")
    }
}
