package com.example.gemmacontrol.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface CapturePreferencesRepository {
    val captureEnabledFlow: Flow<Boolean>
    val storageEnabledFlow: Flow<Boolean>
    val storageEnabledAtFlow: Flow<Long>
    suspend fun setCaptureEnabled(enabled: Boolean)
    suspend fun setStorageEnabled(enabled: Boolean)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capture_settings")

class DataStoreCapturePreferencesRepository(private val context: Context) : CapturePreferencesRepository {

    private object PreferencesKeys {
        val CAPTURE_ENABLED = booleanPreferencesKey("capture_enabled")
        val STORAGE_ENABLED = booleanPreferencesKey("storage_enabled")
        val STORAGE_ENABLED_AT = longPreferencesKey("storage_enabled_at")
    }

    override val captureEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.CAPTURE_ENABLED] ?: true
        }

    override val storageEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.STORAGE_ENABLED] ?: false
        }

    override val storageEnabledAtFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.STORAGE_ENABLED_AT] ?: 0L
        }

    override suspend fun setCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAPTURE_ENABLED] = enabled
        }
    }

    override suspend fun setStorageEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STORAGE_ENABLED] = enabled
            if (enabled) {
                preferences[PreferencesKeys.STORAGE_ENABLED_AT] = System.currentTimeMillis()
            } else {
                preferences[PreferencesKeys.STORAGE_ENABLED_AT] = 0L
            }
        }
    }
}
