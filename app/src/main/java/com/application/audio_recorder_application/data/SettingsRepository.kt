package com.application.audio_recorder_application.data

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore("recording_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val AUDIO_SOURCE_KEY = stringPreferencesKey("audio_source")
        val RECORDING_FORMAT_KEY = stringPreferencesKey("recording_format")
        val SAMPLE_RATE_KEY = stringPreferencesKey("sample_rate")
        val BITRATE_KEY = stringPreferencesKey("bitrate")
        val RECORDINGS_FOLDER_KEY = stringPreferencesKey("recordings_folder")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        
        // Значения по умолчанию
        const val DEFAULT_AUDIO_SOURCE = "По умолчанию"
        const val DEFAULT_RECORDING_FORMAT = "AAC (m4a)"
        const val DEFAULT_SAMPLE_RATE = "44.1 кГц"
        const val DEFAULT_BITRATE = "128 кбит/с"
        const val DEFAULT_LANGUAGE = "По умолчанию"
    }

    // Чтение настроек
    fun getSetting(key: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.map { preferences ->
            when (key) {
                AUDIO_SOURCE_KEY -> preferences[key] ?: DEFAULT_AUDIO_SOURCE
                RECORDING_FORMAT_KEY -> preferences[key] ?: DEFAULT_RECORDING_FORMAT
                SAMPLE_RATE_KEY -> preferences[key] ?: DEFAULT_SAMPLE_RATE
                BITRATE_KEY -> preferences[key] ?: DEFAULT_BITRATE
                RECORDINGS_FOLDER_KEY -> preferences[key] ?: getDefaultRecordingsFolder()
                LANGUAGE_KEY -> preferences[key] ?: DEFAULT_LANGUAGE
                else -> preferences[key] ?: ""
            }
        }
    }

    // Сохранение настроек
    suspend fun saveSetting(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
        Log.d("SettingsRepository", "Сохранена настройка: ${key.name} = $value")
    }
    
    // Получение значения настройки синхронно
    suspend fun getSettingValue(key: Preferences.Key<String>): String {
        return getSetting(key).first()
    }
    
    // Получение папки для записей по умолчанию
    private fun getDefaultRecordingsFolder(): String {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return externalDir?.absolutePath ?: "/storage/emulated/0/Music"
    }
    
    // Применение настроек аудио
    suspend fun applyAudioSettings(
        audioSource: String,
        recordingFormat: String,
        sampleRate: String,
        bitrate: String
    ) {
        // Здесь можно добавить логику для применения настроек к аудиорекордеру
        // Например, обновление конфигурации MediaRecorder
        
        Log.d("SettingsRepository", "Применены настройки аудио:")
        Log.d("SettingsRepository", "- Источник: $audioSource")
        Log.d("SettingsRepository", "- Формат: $recordingFormat")
        Log.d("SettingsRepository", "- Частота: $sampleRate")
        Log.d("SettingsRepository", "- Битрейт: $bitrate")
        
        // Сохраняем настройки в постоянное хранилище
        saveSetting(AUDIO_SOURCE_KEY, audioSource)
        saveSetting(RECORDING_FORMAT_KEY, recordingFormat)
        saveSetting(SAMPLE_RATE_KEY, sampleRate)
        saveSetting(BITRATE_KEY, bitrate)
    }
    
    // Применение настроек хранения
    suspend fun applyStorageSettings(recordingsFolder: String) {
        // Проверяем существование директории и создаем ее при необходимости
        val folder = File(recordingsFolder)
        if (!folder.exists()) {
            val created = folder.mkdirs()
            if (created) {
                Log.d("SettingsRepository", "Создана директория для записей: $recordingsFolder")
            } else {
                Log.e("SettingsRepository", "Не удалось создать директорию: $recordingsFolder")
                // Если не удалось создать директорию, используем директорию по умолчанию
                saveSetting(RECORDINGS_FOLDER_KEY, getDefaultRecordingsFolder())
                return
            }
        }
        
        Log.d("SettingsRepository", "Применены настройки хранения:")
        Log.d("SettingsRepository", "- Папка для записей: $recordingsFolder")
        
        // Сохраняем настройки в постоянное хранилище
        saveSetting(RECORDINGS_FOLDER_KEY, recordingsFolder)
    }
    
    // Применение настроек интерфейса
    suspend fun applyInterfaceSettings(language: String) {
        // Здесь можно добавить логику для применения языковых настроек
        // Например, обновление локали приложения
        
        Log.d("SettingsRepository", "Применены настройки интерфейса:")
        Log.d("SettingsRepository", "- Язык: $language")
        
        // Сохраняем настройки в постоянное хранилище
        saveSetting(LANGUAGE_KEY, language)
    }
    
    // Получение настроек аудио для использования в AudioRecorderRepository
    suspend fun getAudioSettings(): AudioSettings {
        val audioSource = getSettingValue(AUDIO_SOURCE_KEY)
        val recordingFormat = getSettingValue(RECORDING_FORMAT_KEY)
        val sampleRate = getSettingValue(SAMPLE_RATE_KEY)
        val bitrate = getSettingValue(BITRATE_KEY)
        
        return AudioSettings(
            audioSource = parseAudioSource(audioSource),
            outputFormat = parseOutputFormat(recordingFormat),
            audioEncoder = parseAudioEncoder(recordingFormat),
            sampleRate = parseSampleRate(sampleRate),
            bitrate = parseBitrate(bitrate)
        )
    }
    
    // Вспомогательные методы для парсинга настроек
    
    private fun parseAudioSource(source: String): Int {
        return when (source) {
            "Микрофон" -> android.media.MediaRecorder.AudioSource.MIC
            "Камкордер" -> android.media.MediaRecorder.AudioSource.CAMCORDER
            else -> android.media.MediaRecorder.AudioSource.DEFAULT
        }
    }
    
    private fun parseOutputFormat(format: String): Int {
        return when (format) {
            "WAV" -> android.media.MediaRecorder.OutputFormat.THREE_GPP // Используем 3GPP как контейнер для WAV
            "MP3" -> android.media.MediaRecorder.OutputFormat.MPEG_4 // MP3 не поддерживается напрямую, используем MPEG_4
            else -> android.media.MediaRecorder.OutputFormat.MPEG_4 // AAC (m4a)
        }
    }
    
    private fun parseAudioEncoder(format: String): Int {
        return when (format) {
            "WAV" -> android.media.MediaRecorder.AudioEncoder.AMR_NB // Для WAV используем AMR_NB
            "MP3" -> android.media.MediaRecorder.AudioEncoder.AAC // MP3 не поддерживается напрямую, используем AAC
            else -> android.media.MediaRecorder.AudioEncoder.AAC // AAC (m4a)
        }
    }
    
    private fun parseSampleRate(sampleRate: String): Int {
        return when (sampleRate) {
            "8 кГц" -> 8000
            "16 кГц" -> 16000
            "44.1 кГц" -> 44100
            else -> 44100
        }
    }
    
    private fun parseBitrate(bitrate: String): Int {
        return when (bitrate) {
            "64 кбит/с" -> 64000
            "128 кбит/с" -> 128000
            "256 кбит/с" -> 256000
            else -> 128000
        }
    }
    
    // Класс для хранения настроек аудио
    data class AudioSettings(
        val audioSource: Int,
        val outputFormat: Int,
        val audioEncoder: Int,
        val sampleRate: Int,
        val bitrate: Int
    )
}
