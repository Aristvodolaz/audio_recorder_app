package com.application.audio_recorder_application.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("recording_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val AUDIO_SOURCE_KEY = stringPreferencesKey("audio_source")
        val RECORDING_FORMAT_KEY = stringPreferencesKey("recording_format")
        val SAMPLE_RATE_KEY = stringPreferencesKey("sample_rate")
        val BITRATE_KEY = stringPreferencesKey("bitrate")
        val RECORDINGS_FOLDER_KEY = stringPreferencesKey("recordings_folder")
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    // Чтение настроек
    fun getSetting(key: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }

    // Сохранение настроек
    suspend fun saveSetting(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
