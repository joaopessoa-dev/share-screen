package com.example.sharescreen.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "share_screen_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val TOKEN_EXPIRY_KEY = longPreferencesKey("token_expiry")
    }

    val deviceId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEVICE_ID_KEY] ?: ""
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN_KEY]
    }

    val tokenExpiry: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_EXPIRY_KEY] ?: 0L
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID_KEY]
        if (existing != null) return existing

        val newDeviceId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID_KEY] = newDeviceId
        }
        return newDeviceId
    }

    suspend fun saveToken(token: String, expiresInSeconds: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY] = token
            prefs[TOKEN_EXPIRY_KEY] = expiryTime
        }
    }

    suspend fun getValidToken(): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[AUTH_TOKEN_KEY] ?: return null
        val expiry = prefs[TOKEN_EXPIRY_KEY] ?: 0L

        // Token expirado ou expira em menos de 5 minutos
        if (System.currentTimeMillis() > expiry - (5 * 60 * 1000)) {
            return null
        }
        return token
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(TOKEN_EXPIRY_KEY)
        }
    }
}
