package com.application.audio_recorder_application.viewmodel

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.audio_recorder_application.data.AudioRecorderRepository
import com.application.audio_recorder_application.data.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

// Перечисление состояний записи
enum class RecordingState {
    IDLE,       // Бездействующая (не записывает)
    RECORDING,  // Идет запись
    PAUSED      // Запись на паузе
}

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRecorderRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _recordingsList = MutableStateFlow<List<File>>(emptyList())
    val recordingsList: StateFlow<List<File>> get() = _recordingsList

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> get() = _isPaused

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    private val _currentPlaybackTime = MutableStateFlow(0L)
    val currentPlaybackTime: StateFlow<Long> get() = _currentPlaybackTime

    private val _currentPlaybackDuration = MutableStateFlow(0L)
    val currentPlaybackDuration: StateFlow<Long> get() = _currentPlaybackDuration

    private val _amplitude = MutableStateFlow(0)
    val amplitude = _amplitude.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(-1)

    private val sampleRate = 16000 // Частота дискретизации

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> get() = _recordingState

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> get() = _currentFilePath

    private val _snackbarMessage = MutableStateFlow<String?>("Готово")
    val snackbarMessage: StateFlow<String?> get() = _snackbarMessage

    // Таймер записи
    private val _seconds = MutableStateFlow(0)
    val seconds: StateFlow<Int> get() = _seconds
    
    // Состояние разрешения на запись
    val permissionGranted = MutableStateFlow(false)
    
    // Launcher для запроса разрешения - инициализируется в RecorderScreen
    var permissionLauncher: ActivityResultLauncher<String>? = null

    private var timerJob: Job? = null
    
    init {
        loadRecordings()
    }

    fun loadRecordings() {
        _recordingsList.value = repository.getRecordingsList()
        Log.d("AudioViewModel", "Loaded recordings: ${_recordingsList.value.size} files")
    }

    // Метод для начала записи без указания пути (генерирует путь автоматически)
    fun startRecording() {
        viewModelScope.launch {
            // Проверяем разрешение на запись
            if (!permissionGranted.value) {
                permissionLauncher?.launch(android.Manifest.permission.RECORD_AUDIO)
                _snackbarMessage.value = "Для записи необходимо разрешение на использование микрофона"
                return@launch
            }
            
            // Генерируем имя файла на основе текущей даты и времени
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Recording_$timestamp.m4a"
            
            // Получаем путь к директории для записей
            val recordingsDir = File(repository.getRecordingsDirectory())
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            // Полный путь к файлу
            val outputFilePath = File(recordingsDir, fileName).absolutePath
            
            // Запускаем запись с указанным путем
            startRecording(outputFilePath)
        }
    }

    fun startRecording(outputFilePath: String) {
        viewModelScope.launch {
            // Проверка доступного места
            if (repository.checkStorageSpace()) {
                repository.startRecording(outputFilePath)
                
                // Обновляем состояние
                _isRecording.value = true
                _isPaused.value = false
                _recordingState.value = RecordingState.RECORDING
                _currentFilePath.value = outputFilePath
                
                // Запускаем таймер
                startTimer()
                
                // Запускаем обновление амплитуды
                updateAmplitude()
                
                // Информационное сообщение
                _snackbarMessage.value = "Запись начата"
                Log.d("AudioViewModel", "Recording started at $outputFilePath")
            } else {
                // Показываем сообщение о нехватке места
                _snackbarMessage.value = "Ошибка: Недостаточно места для записи"
                Log.e("AudioViewModel", "Not enough storage space to record")
            }
        }
    }

    private fun updateAmplitude() {
        viewModelScope.launch {
            while (_isRecording.value) {
                val amplitude = repository.getRecorderAmplitude()
                _amplitude.value = amplitude
                delay(50) // Обновляем амплитуду каждые 50 мс
            }
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            repository.pauseRecording()
            _isPaused.value = true
            _recordingState.value = RecordingState.PAUSED
            _snackbarMessage.value = "Запись приостановлена"
            timerJob?.cancel()
            Log.d("AudioViewModel", "Recording paused")
        }
    }

    fun resumeRecording() {
        if (_isPaused.value) {
            repository.resumeRecording()
            _isRecording.value = true
            _isPaused.value = false
            _recordingState.value = RecordingState.RECORDING
            _snackbarMessage.value = "Запись возобновлена"
            continueTimer()
            Log.d("AudioViewModel", "Recording resumed")
        }
    }

    fun completeRecording() {
        if (_isRecording.value || _isPaused.value) {
            stopRecording()
            _isRecording.value = false
            _isPaused.value = false
            _recordingState.value = RecordingState.IDLE
            
            // Сохраняем запись в базу данных
            _currentFilePath.value?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    viewModelScope.launch {
                        try {
                            // Получаем имя файла из пути
                            val fileName = file.name
                            
                            // Сохраняем запись в базу данных
                            recordingRepository.saveRecording(
                                filePath = filePath,
                                fileName = fileName,
                                category = "Общее"
                            )
                            
                            _snackbarMessage.value = "Запись сохранена"
                            Log.d("AudioViewModel", "Recording saved to database: $fileName")
                        } catch (e: Exception) {
                            Log.e("AudioViewModel", "Error saving recording to database: ${e.message}")
                            _snackbarMessage.value = "Ошибка сохранения записи: ${e.message}"
                        }
                    }
                }
            }
            
            // Обновляем список записей после сохранения новой записи
            viewModelScope.launch {
                // Небольшая задержка, чтобы файл успел сохраниться
                delay(500)
                loadRecordings()
            }
            
            Log.d("AudioViewModel", "Recording completed and saved")
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        repository.stopRecording()
        Log.d("AudioViewModel", "Recording stopped")
    }

    fun cancelRecording() {
        if (_isRecording.value || _isPaused.value) {
            // Останавливаем запись
            stopRecording()
            
            // Удаляем текущий файл записи
            _currentFilePath.value?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    if (file.delete()) {
                        Log.d("AudioViewModel", "Recording cancelled and file deleted: $filePath")
                    } else {
                        Log.e("AudioViewModel", "Failed to delete recording file: $filePath")
                    }
                }
            }
            
            // Сбрасываем состояние
            _isRecording.value = false
            _isPaused.value = false
            _recordingState.value = RecordingState.IDLE
            _seconds.value = 0
            _currentFilePath.value = null
            
            // Показываем сообщение пользователю
            _snackbarMessage.value = "Запись отменена"
            
            Log.d("AudioViewModel", "Recording cancelled")
        }
    }

    fun playRecording(recording: File) {
        // Останавливаем предыдущее воспроизведение, если оно есть
        if (_isPlaying.value) {
            stopPlayback()
        }
        
        // Устанавливаем текущий индекс трека
        val trackIndex = _recordingsList.value.indexOf(recording)
        if (trackIndex != -1) {
            _currentTrackIndex.value = trackIndex
            Log.d("AudioViewModel", "Playing track #${trackIndex + 1} of ${_recordingsList.value.size}")
        }
        
        repository.playRecording(recording, viewModelScope)
        _isPlaying.value = true
        _currentPlaybackDuration.value = getRecordingDuration(recording)
        _currentPlaybackTime.value = 0L
        startPlaybackTimer()
        Log.d("AudioViewModel", "Playback started: ${recording.absolutePath}")
    }

    fun pausePlayback() {
        repository.pausePlayback()
        _isPlaying.value = false
        Log.d("AudioViewModel", "Playback paused")
    }

    fun resumePlayback() {
        repository.resumePlayback()
        _isPlaying.value = true
        startPlaybackTimer()
        Log.d("AudioViewModel", "Playback resumed")
    }

    fun seekTo(position: Long) {
        repository.seekTo(position)
        _currentPlaybackTime.value = position
        Log.d("AudioViewModel", "Seeked to position: $position ms")
    }

    fun stopPlayback() {
        repository.stopPlayback()
        _isPlaying.value = false
        _currentPlaybackTime.value = 0L
        Log.d("AudioViewModel", "Playback stopped")
    }

    fun previousTrack() {
        if (_currentTrackIndex.value > 0) {
            _currentTrackIndex.value--
            val previousTrack = _recordingsList.value[_currentTrackIndex.value]
            playRecording(previousTrack)
            Log.d("AudioViewModel", "Switched to previous track: ${previousTrack.name}")
        }
    }

    fun nextTrack() {
        if (_currentTrackIndex.value < _recordingsList.value.lastIndex) {
            _currentTrackIndex.value++
            val nextTrack = _recordingsList.value[_currentTrackIndex.value]
            playRecording(nextTrack)
            Log.d("AudioViewModel", "Switched to next track: ${nextTrack.name}")
        }
    }

    private fun startPlaybackTimer() {
        viewModelScope.launch {
            _currentPlaybackTime.value = 0L // Сбрасываем время при начале воспроизведения
            
            while (_isPlaying.value) {
                delay(100L) // Обновляем чаще для более плавной визуализации
                
                // Получаем актуальное положение из репозитория
                val currentPosition = repository.getCurrentPlaybackPosition()
                _currentPlaybackTime.value = currentPosition
                
                // Если достигли конца записи, останавливаем воспроизведение
                if (currentPosition >= _currentPlaybackDuration.value && _currentPlaybackDuration.value > 0) {
                    _isPlaying.value = false
                    _currentPlaybackTime.value = 0L
                    
                    // Автоматический переход к следующему треку
                    if (_currentTrackIndex.value < _recordingsList.value.lastIndex) {
                        delay(500L) // Небольшая пауза между треками
                        _currentTrackIndex.value++
                        val nextTrack = _recordingsList.value[_currentTrackIndex.value]
                        playRecording(nextTrack)
                    }
                    
                    break
                }
            }
        }
    }

    private fun getRecordingDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        retriever.release()
        return durationMs
    }

    fun deleteRecording(file: File) {
        if (repository.deleteRecording(file)) {
            loadRecordings()
            Log.d("AudioViewModel", "Recording deleted: ${file.absolutePath}")
        } else {
            Log.w("AudioViewModel", "Failed to delete: file not found - ${file.absolutePath}")
        }
    }

    // Метод для очистки сообщений
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    // Функция для запуска таймера с нуля
    private fun startTimer() {
        _seconds.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _seconds.value = _seconds.value + 1
                }
            }
        }
    }
    
    // Функция для продолжения таймера после паузы
    private fun continueTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _seconds.value = _seconds.value + 1
                }
            }
        }
    }

    // Метод для получения доступного места на устройстве
    fun getAvailableStorage(): Long {
        return repository.getAvailableStorage()
    }
    
    // Метод для получения текущего размера файла записи
    fun getCurrentFileSize(): Long {
        return repository.getCurrentFileSize()
    }
}
