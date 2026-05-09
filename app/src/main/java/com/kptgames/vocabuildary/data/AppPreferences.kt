package com.kptgames.vocabuildary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.vocabuildaryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vocabuildary_prefs"
)

class AppPreferences(private val context: Context) {
    companion object {
        private val AUTH_STATE_KEY = stringPreferencesKey("auth_state_json")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("gateway_auth_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val REMINDER_SLOTS_KEY = stringPreferencesKey("reminder_slots_json")
    }

    suspend fun getGatewayAuthToken(): String? {
        return context.vocabuildaryDataStore.data.first()[AUTH_TOKEN_KEY]
    }

    suspend fun saveGatewayAuthToken(value: String) {
        context.vocabuildaryDataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = value
        }
    }

    suspend fun clearGatewayAuthToken() {
        context.vocabuildaryDataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(AUTH_STATE_KEY)
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.vocabuildaryDataStore.data.first()[DEVICE_ID_KEY]
        if (!current.isNullOrBlank()) return current
        val generated = UUID.randomUUID().toString()
        context.vocabuildaryDataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = generated
        }
        return generated
    }

    suspend fun saveReminderSlotsJson(value: String) {
        context.vocabuildaryDataStore.edit { preferences ->
            preferences[REMINDER_SLOTS_KEY] = value
        }
    }

    suspend fun getReminderSlotsJson(): String? {
        return context.vocabuildaryDataStore.data.first()[REMINDER_SLOTS_KEY]
    }

    suspend fun getAuthStateJson(): String? {
        return context.vocabuildaryDataStore.data.first()[AUTH_STATE_KEY]
    }

    suspend fun saveAuthStateJson(value: String) {
        context.vocabuildaryDataStore.edit { preferences ->
            preferences[AUTH_STATE_KEY] = value
        }
    }

    suspend fun clearAuthState() {
        context.vocabuildaryDataStore.edit { preferences ->
            preferences.remove(AUTH_STATE_KEY)
        }
    }
}
