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

@HiltViewModel
class AudioViewModel @Inject constructor(private val repository: AudioRecorderRepository) : ViewModel() {

    private val _recordingsList = MutableStateFlow<List<File>>(emptyList())
    val recordingsList: StateFlow<List<File>> get() = _recordingsList

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private val _amplitude = MutableStateFlow(0)
    val amplitude = _amplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000 // Частота дискретизации

    init {
        loadRecordings()
    }

    /**
     * Загружает список записей из репозитория
     */
    fun loadRecordings() {
        _recordingsList.value = repository.getRecordingsList()
    }

    /**
     * Начинает запись в указанный файл
     */
    fun startRecording(outputFilePath: String) {
        repository.startRecording(outputFilePath)
        _isRecording.value = true

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

    /**
     * Обновляет амплитуду во время записи
     */
    private fun updateAmplitude() {
        viewModelScope.launch {
            val buffer = ShortArray(1024)
            while (_isRecording.value && audioRecord != null && audioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSize = audioRecord!!.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    val maxAmplitude = buffer.maxOrNull()?.toInt() ?: 0
                    _amplitude.value = maxAmplitude
                }
                delay(50) // Обновляем амплитуду каждые 50 мс
            }
        }
    }

    /**
     * Останавливает запись
     */
    fun stopRecording() {
        repository.stopRecording()
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        loadRecordings() // Обновляем список после завершения записи
    }

    // Остальные функции для паузы, воспроизведения и удаления записей
    fun pauseRecording() {
        repository.pauseRecording()
        _isRecording.value = false
    }

    fun resumeRecording() {
        repository.resumeRecording()
        _isRecording.value = true
    }

    fun playRecording(file: File) {
        repository.playRecording(file)
    }

    fun deleteRecording(file: File) {
        if (repository.deleteRecording(file)) {
            loadRecordings()
        }
    }
}
