package com.application.audio_recorder_application.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.audio_recorder_application.data.RecordingRepository
import com.application.audio_recorder_application.data.model.Recording
import com.application.audio_recorder_application.util.EmotionRecognitionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val emotionRecognitionService: EmotionRecognitionService
) : ViewModel() {

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _emotionAnalysisResult = MutableStateFlow<EmotionRecognitionService.EmotionAnalysisResult?>(null)
    val emotionAnalysisResult: StateFlow<EmotionRecognitionService.EmotionAnalysisResult?> = _emotionAnalysisResult.asStateFlow()

    init {
        loadRecordings()
        loadCategories()
    }

    fun loadRecordings() {
        viewModelScope.launch {
            try {
                when {
                    _searchQuery.value.isNotEmpty() -> {
                        recordingRepository.searchRecordings(_searchQuery.value).collectLatest {
                            _recordings.value = it
                        }
                    }
                    _selectedCategory.value != null -> {
                        recordingRepository.getRecordingsByCategory(_selectedCategory.value!!).collectLatest {
                            _recordings.value = it
                        }
                    }
                    else -> {
                        recordingRepository.getAllRecordings().collectLatest {
                            _recordings.value = it
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки записей: ${e.message}"
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                recordingRepository.getAllCategories().collectLatest {
                    _categories.value = it
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки категорий: ${e.message}"
            }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        loadRecordings()
    }

    fun selectRecording(recording: Recording) {
        _selectedRecording.value = recording
        // Сбрасываем результат анализа эмоций при выборе новой записи
        _emotionAnalysisResult.value = null
    }

    fun search(query: String) {
        _searchQuery.value = query
        loadRecordings()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        loadRecordings()
    }

    fun saveRecording(filePath: String, fileName: String, category: String, tags: List<String>) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                recordingRepository.saveRecording(filePath, fileName, category, tags)
                loadRecordings()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сохранения записи: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                recordingRepository.deleteRecording(recording)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = null
                    _emotionAnalysisResult.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления записи: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun transcribeRecording(recording: Recording) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val updatedRecording = recordingRepository.transcribeRecording(recording)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка распознавания речи: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun analyzeEmotions(recording: Recording) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val audioFile = File(recording.filePath)
                if (!audioFile.exists() || recording.isEncrypted) {
                    _errorMessage.value = "Невозможно анализировать эмоции: файл не существует или зашифрован"
                    return@launch
                }
                
                val result = emotionRecognitionService.analyzeEmotions(audioFile)
                _emotionAnalysisResult.value = result
                
                // Добавляем эмоцию в теги записи, если её там ещё нет
                val emotionTag = "эмоция:${result.primaryEmotion.name.lowercase()}"
                if (!recording.tags.contains(emotionTag)) {
                    val updatedTags = recording.tags.toMutableList().apply {
                        // Удаляем старые теги эмоций
                        removeAll { it.startsWith("эмоция:") }
                        // Добавляем новый тег эмоции
                        add(emotionTag)
                    }
                    
                    val updatedRecording = recording.copy(tags = updatedTags)
                    recordingRepository.updateRecording(updatedRecording)
                    
                    if (_selectedRecording.value?.id == recording.id) {
                        _selectedRecording.value = updatedRecording
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка анализа эмоций: ${e.message}"
                _emotionAnalysisResult.value = null
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun applyAudioEffect(recording: Recording, effectType: RecordingRepository.AudioEffectType) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val updatedRecording = recordingRepository.applyAudioEffect(recording, effectType)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                    // Сбрасываем результат анализа эмоций при изменении аудио
                    _emotionAnalysisResult.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка применения эффекта: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun trimAudio(recording: Recording, startTimeSeconds: Int, durationSeconds: Int) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val updatedRecording = recordingRepository.trimAudio(recording, startTimeSeconds, durationSeconds)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                    // Сбрасываем результат анализа эмоций при изменении аудио
                    _emotionAnalysisResult.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обрезки аудио: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun convertFormat(recording: Recording, format: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val updatedRecording = recordingRepository.convertFormat(recording, format)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка конвертации формата: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun mergeRecordings(recordings: List<Recording>, outputFileName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                recordingRepository.mergeRecordings(recordings, outputFileName)
                loadRecordings()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка объединения записей: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleEncryption(recording: Recording) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val updatedRecording = if (recording.isEncrypted) {
                    recordingRepository.decryptRecording(recording)
                } else {
                    recordingRepository.encryptRecording(recording)
                }
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                    // Сбрасываем результат анализа эмоций при шифровании/дешифровании
                    if (updatedRecording.isEncrypted) {
                        _emotionAnalysisResult.value = null
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка шифрования/дешифрования: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            try {
                val updatedRecording = recordingRepository.toggleFavorite(recording)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка изменения статуса избранного: ${e.message}"
            }
        }
    }

    fun addNote(recording: Recording, note: String) {
        viewModelScope.launch {
            try {
                val updatedRecording = recordingRepository.addNote(recording, note)
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = updatedRecording
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка добавления заметки: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearEmotionAnalysisResult() {
        _emotionAnalysisResult.value = null
    }
} 