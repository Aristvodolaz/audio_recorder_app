package com.application.audio_recorder_application.viewmodel

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.audio_recorder_application.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {

    private val _audioSource = MutableStateFlow("Default")
    val audioSource: StateFlow<String> get() = _audioSource.asStateFlow()

    private val _recordingFormat = MutableStateFlow("AAC (m4a)")
    val recordingFormat: StateFlow<String> get() = _recordingFormat.asStateFlow()

    private val _sampleRate = MutableStateFlow("16 kHz")
    val sampleRate: StateFlow<String> get() = _sampleRate.asStateFlow()

    private val _bitrate = MutableStateFlow("128 kbps")
    val bitrate: StateFlow<String> get() = _bitrate.asStateFlow()

    private val _recordingsFolder = MutableStateFlow("/storage/emulated/0/VoiceRecorder")
    val recordingsFolder: StateFlow<String> get() = _recordingsFolder.asStateFlow()

    private val _language = MutableStateFlow("Default")
    val language: StateFlow<String> get() = _language.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.AUDIO_SOURCE_KEY).collect { _audioSource.value = it }
        }
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.RECORDING_FORMAT_KEY).collect { _recordingFormat.value = it }
        }
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.SAMPLE_RATE_KEY).collect { _sampleRate.value = it }
        }
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.BITRATE_KEY).collect { _bitrate.value = it }
        }
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.RECORDINGS_FOLDER_KEY).collect { _recordingsFolder.value = it }
        }
        viewModelScope.launch {
            repository.getSetting(SettingsRepository.LANGUAGE_KEY).collect { _language.value = it }
        }
    }

    fun saveSetting(key: Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }
    
    // Метод для применения всех настроек
    fun applyAllSettings() {
        viewModelScope.launch {
            // Применяем настройки аудио
            repository.applyAudioSettings(
                audioSource = _audioSource.value,
                recordingFormat = _recordingFormat.value,
                sampleRate = _sampleRate.value,
                bitrate = _bitrate.value
            )
            
            // Применяем настройки хранения
            repository.applyStorageSettings(
                recordingsFolder = _recordingsFolder.value
            )
            
            // Применяем настройки интерфейса
            repository.applyInterfaceSettings(
                language = _language.value
            )
        }
    }
}
