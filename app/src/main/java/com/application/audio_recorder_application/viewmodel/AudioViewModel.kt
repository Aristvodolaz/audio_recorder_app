package com.application.audio_recorder_application.viewmodel

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.audio_recorder_application.data.AudioRecorderRepository
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

@HiltViewModel
class AudioViewModel @Inject constructor(private val repository: AudioRecorderRepository) : ViewModel() {

    private val _recordingsList = MutableStateFlow<List<File>>(emptyList())
    val recordingsList: StateFlow<List<File>> get() = _recordingsList

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> get() = _isPaused

    private val _amplitude = MutableStateFlow(0)
    val amplitude = _amplitude.asStateFlow()

    // SharedFlow для управления уведомлением о завершении записи
    private val _showSnackbar = MutableSharedFlow<Boolean>()
    val showSnackbar: SharedFlow<Boolean> = _showSnackbar.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var outputFilePath: String? = null
    private val sampleRate = 16000

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        _recordingsList.value = repository.getRecordingsList()
    }

    fun startRecording(filePath: String) {
        outputFilePath = filePath
        repository.startRecording(filePath)
        _isRecording.value = true
        _isPaused.value = false

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        )

        audioRecord?.startRecording()
        updateAmplitude()
    }

    private fun updateAmplitude() {
        viewModelScope.launch {
            val buffer = ShortArray(1024)
            while (_isRecording.value && !_isPaused.value && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSize = audioRecord!!.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    val maxAmplitude = buffer.maxOrNull()?.toInt() ?: 0
                    _amplitude.value = maxAmplitude
                }
                delay(50)
            }
        }
    }

    fun pauseRecording() {
        _isPaused.value = true
        audioRecord?.stop()
    }

    fun resumeRecording() {
        if (_isPaused.value) {
            _isPaused.value = false
            audioRecord?.startRecording()
            updateAmplitude()
        }
    }

    fun completeRecording() {
        _isRecording.value = false
        _isPaused.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        repository.stopRecording()
        loadRecordings() // Обновляем список после завершения записи

        // Уведомление об успешной записи
        viewModelScope.launch {
            _showSnackbar.emit(true) // Показываем Snackbar
        }
    }

    fun playRecording(file: File) {
        repository.playRecording(file)
    }

    fun deleteRecording(file: File) {
        if (repository.deleteRecording(file)) {
            loadRecordings() // Обновляем список после удаления записи
        }
    }
}
