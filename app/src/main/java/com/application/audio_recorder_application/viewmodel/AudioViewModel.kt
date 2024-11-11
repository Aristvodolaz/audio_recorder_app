package com.application.audio_recorder_application.viewmodel

import AudioRecorderRepository
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.audio_recorder_application.util.SpeechRecognitionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val speechRecognitionHelper: SpeechRecognitionHelper,
    private val audioRecorderRepository: AudioRecorderRepository
) : ViewModel() {

    private val _transcriptionResult = MutableStateFlow("Waiting for transcription...")
    val transcriptionResult: StateFlow<String> get() = _transcriptionResult

    private val _isRecording = audioRecorderRepository.isRecordingFlow
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private val _recordingsList = MutableStateFlow<List<File>>(emptyList())
    val recordingsList: StateFlow<List<File>> get() = _recordingsList

    private val _volumeLevel = MutableStateFlow(0.5f)
    val volumeLevel: StateFlow<Float> get() = _volumeLevel

    val isPaused = MutableStateFlow(false)

    fun updateVolumeLevel(volume: Float) {
        _volumeLevel.value = volume
    }

    fun togglePauseResume() {
        if (isPaused.value) {
            audioRecorderRepository.resumeRecording()
        } else {
            audioRecorderRepository.pauseRecording()
        }
        isPaused.value = !isPaused.value
    }

    fun loadRecordings() {
        viewModelScope.launch {
            _recordingsList.value = audioRecorderRepository.getRecordingsList()
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                audioRecorderRepository.startRecording("recorded_audio")
                _transcriptionResult.value = "Recording started..."
            } catch (e: Exception) {
                _transcriptionResult.value = "Recording failed: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                audioRecorderRepository.stopRecording()
                _transcriptionResult.value = "Recording stopped."
            } catch (e: Exception) {
                _transcriptionResult.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun startTranscription(language: String) {
        viewModelScope.launch {
            try {
                speechRecognitionHelper.startListening(language)
                _transcriptionResult.value = "Listening for transcription..."
            } catch (e: Exception) {
                _transcriptionResult.value = "Transcription failed: ${e.message}"
            }
        }
    }

    fun playRecording(file: File) {
        audioRecorderRepository.playRecording(file)
    }

    fun stopTranscription() {
        try {
            speechRecognitionHelper.stopListening()
            _transcriptionResult.value = "Transcription stopped."
        } catch (e: Exception) {
            _transcriptionResult.value = "Failed to stop transcription: ${e.message}"
        }
    }

    fun updateTranscriptionResult(message: String) {
        _transcriptionResult.value = message
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionHelper.release()
    }
}
