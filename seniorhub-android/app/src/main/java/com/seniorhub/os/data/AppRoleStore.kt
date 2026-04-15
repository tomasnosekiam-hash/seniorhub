package com.seniorhub.os.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appRoleDataStore by preferencesDataStore(name = "app_role")

class AppRoleStore(
    private val context: Context,
) {

    suspend fun getRoleOrNull(): AppRole? {
        val raw = context.appRoleDataStore.data.first()[KEY_ROLE] ?: return null
        return when (raw) {
            "senior" -> AppRole.Senior
            "admin" -> AppRole.Admin
            else -> null
        }
    }

    suspend fun setRole(role: AppRole) {
        val value = when (role) {
            AppRole.Senior -> "senior"
            AppRole.Admin -> "admin"
        }
        context.appRoleDataStore.edit { prefs ->
            prefs[KEY_ROLE] = value
        }
    }

    private companion object {
        val KEY_ROLE = stringPreferencesKey("role")
    }
}
