package com.seniorhub.os.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.inboundCellularDedupStore: DataStore<Preferences> by preferencesDataStore(
    name = "inbound_sms_dedup",
)

/** Klíče typu `sms:12` / `mms:3` — zprávy už zrcadlené do Firestore. */
class InboundCellularDedupStore(private val context: Context) {

    suspend fun contains(dedupKey: String): Boolean =
        context.inboundCellularDedupStore.data
            .map { prefs -> prefs[KEY_IDS].orEmpty().split(',').any { it == dedupKey } }
            .first()

    suspend fun markProcessed(dedupKey: String) {
        context.inboundCellularDedupStore.edit { prefs ->
            val current = prefs[KEY_IDS].orEmpty().split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            current.add(dedupKey)
            val trimmed = current.toList().takeLast(MAX_TRACKED)
            prefs[KEY_IDS] = trimmed.joinToString(",")
        }
    }

    companion object {
        private val KEY_IDS = stringPreferencesKey("processed_telephony_ids")
        private const val MAX_TRACKED = 500
    }
}
