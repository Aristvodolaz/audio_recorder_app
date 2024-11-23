package com.application.audio_recorder_application.viewmodel

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.util.Log
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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    private val _currentPlaybackTime = MutableStateFlow(0L)
    val currentPlaybackTime: StateFlow<Long> get() = _currentPlaybackTime

    private val _currentPlaybackDuration = MutableStateFlow(0L)
    val currentPlaybackDuration: StateFlow<Long> get() = _currentPlaybackDuration

    private val _amplitude = MutableStateFlow(0)
    val amplitude = _amplitude.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(-1)

    private var audioRecord: AudioRecord? = null

    private val sampleRate = 16000 // Частота дискретизации

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        _recordingsList.value = repository.getRecordingsList()
        Log.d("AudioViewModel", "Loaded recordings: ${_recordingsList.value.size} files")
    }

    fun startRecording(filePath: String) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        )
        audioRecord?.startRecording()
        _isRecording.value = true
        updateAmplitude()
        Log.d("AudioViewModel", "Recording started: $filePath")
    }

    private fun updateAmplitude() {
        viewModelScope.launch {
            val buffer = ShortArray(1024)
            while (_isRecording.value) {
                val maxAmplitude = audioRecord?.read(buffer, 0, buffer.size)?.let {
                    buffer.maxOrNull()?.toInt() ?: 0
                } ?: 0
                _amplitude.value = maxAmplitude
                delay(50) // Обновляем амплитуду каждые 50 мс
            }
        }
    }
    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            audioRecord?.stop()
            _isPaused.value = true
            Log.d("AudioViewModel", "Recording paused")
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            audioRecord?.startRecording()
            _isPaused.value = false
            updateAmplitude()
            Log.d("AudioViewModel", "Recording resumed")
        }
    }
    fun completeRecording() {
        if (_isRecording.value) {
            stopRecording()
            _isRecording.value = false
            Log.d("AudioViewModel", "Recording completed and saved")
        }
    }

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _isRecording.value = false
        Log.d("AudioViewModel", "Recording stopped")
    }

    fun playRecording(recording: File) {
        repository.playRecording(recording, viewModelScope)
        _isPlaying.value = true
        _currentPlaybackDuration.value = getRecordingDuration(recording)
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
            while (_isPlaying.value && _currentPlaybackTime.value < _currentPlaybackDuration.value) {
                delay(1000L)
                _currentPlaybackTime.value += 1000L
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
}
