package com.example.gemmacontrol.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class VoiceInputMode(val storedValue: String) {
    TapToggle("tap_toggle"),
    HoldToSpeak("hold_to_speak");

    companion object {
        fun fromStoredValue(value: String?): VoiceInputMode {
            return entries.firstOrNull { it.storedValue == value } ?: TapToggle
        }
    }
}

interface CapturePreferencesRepository {
    val captureEnabledFlow: Flow<Boolean>
    val storageEnabledFlow: Flow<Boolean>
    val storageEnabledAtFlow: Flow<Long>
    val xiaomiAutostartAcknowledgedFlow: Flow<Boolean>
    val voiceInputModeFlow: Flow<VoiceInputMode>
    suspend fun setCaptureEnabled(enabled: Boolean)
    suspend fun setStorageEnabled(enabled: Boolean)
    suspend fun setXiaomiAutostartAcknowledged(acknowledged: Boolean)
    suspend fun setVoiceInputMode(mode: VoiceInputMode)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capture_settings")

class DataStoreCapturePreferencesRepository(private val context: Context) : CapturePreferencesRepository {

    private object PreferencesKeys {
        val CAPTURE_ENABLED = booleanPreferencesKey("capture_enabled")
        val STORAGE_ENABLED = booleanPreferencesKey("storage_enabled")
        val STORAGE_ENABLED_AT = longPreferencesKey("storage_enabled_at")
        val XIAOMI_AUTOSTART_ACKNOWLEDGED = booleanPreferencesKey("xiaomi_autostart_acknowledged")
        val VOICE_INPUT_MODE = stringPreferencesKey("voice_input_mode")
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

    override val xiaomiAutostartAcknowledgedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.XIAOMI_AUTOSTART_ACKNOWLEDGED] ?: false
        }

    override val voiceInputModeFlow: Flow<VoiceInputMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            VoiceInputMode.fromStoredValue(preferences[PreferencesKeys.VOICE_INPUT_MODE])
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

    override suspend fun setXiaomiAutostartAcknowledged(acknowledged: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.XIAOMI_AUTOSTART_ACKNOWLEDGED] = acknowledged
        }
    }

    override suspend fun setVoiceInputMode(mode: VoiceInputMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_INPUT_MODE] = mode.storedValue
        }
    }
}
