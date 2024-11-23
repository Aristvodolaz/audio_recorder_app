package com.application.audio_recorder_application.data

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class AudioRecorderRepository @Inject constructor(private val context: Context, ) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow = _isRecordingFlow.asStateFlow()

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow = _isPlayingFlow.asStateFlow()

    private val _currentPlaybackPosition = MutableStateFlow(0)
    val currentPlaybackPosition = _currentPlaybackPosition.asStateFlow()

    private var isPaused = false

    fun startRecording(
        outputFilePath: String,
        format: Int = MediaRecorder.OutputFormat.THREE_GPP,
        sampleRate: Int = 16000,
        bitrate: Int = 128000
    ) {
        stopRecording() // Останавливаем любую активную запись перед началом новой

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(format)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(sampleRate)
                setAudioEncodingBitRate(bitrate)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
            _isRecordingFlow.value = true
            isPaused = false
            Log.d("AudioRecorderRepository", "Recording started: $outputFilePath")
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecordingFlow.value = false
            releaseRecorder()
            Log.e("AudioRecorderRepository", "Error starting recording: ${e.message}")
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                _isRecordingFlow.value = false
                Log.d("AudioRecorderRepository", "Recording paused")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AudioRecorderRepository", "Error pausing recording: ${e.message}")
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                _isRecordingFlow.value = true
                Log.d("AudioRecorderRepository", "Recording resumed")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AudioRecorderRepository", "Error resuming recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            Log.d("AudioRecorderRepository", "Recording stopped")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AudioRecorderRepository", "Error stopping recording: ${e.message}")
        } finally {
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        mediaRecorder = null
        isPaused = false
        _isRecordingFlow.value = false
    }

    fun playRecording(file: File, scope: CoroutineScope) {
        stopPlayback() // Останавливаем текущее воспроизведение перед началом нового

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                _isPlayingFlow.value = true

                setOnCompletionListener {
                    stopPlayback()
                }
            }

            scope.launch {
                try {
                    while (_isPlayingFlow.value) {
                        _currentPlaybackPosition.value = mediaPlayer?.currentPosition ?: 0
                        delay(1000L)
                    }
                } catch (e: Exception) {
                    Log.e("AudioRecorderRepository", "Error updating playback position: ${e.message}")
                }
            }
            Log.d("AudioRecorderRepository", "Playback started: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            Log.e("AudioRecorderRepository", "Error starting playback: ${e.message}")
        }
    }

    fun pausePlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlayingFlow.value = false
            Log.d("AudioRecorderRepository", "Playback paused")
        }
    }

    fun resumePlayback() {
        if (mediaPlayer != null && !_isPlayingFlow.value) {
            mediaPlayer?.start()
            _isPlayingFlow.value = true
            Log.d("AudioRecorderRepository", "Playback resumed")
        }
    }


    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        _isPlayingFlow.value = false
        _currentPlaybackPosition.value = 0
        Log.d("AudioRecorderRepository", "Playback stopped")
    }

    fun deleteRecording(file: File): Boolean {
        return if (file.exists()) {
            file.delete().also { success ->
                Log.d("AudioRecorderRepository", "Deleted recording: ${file.absolutePath} - Success: $success")
            }
        } else {
            Log.w("AudioRecorderRepository", "File does not exist: ${file.absolutePath}")
            false
        }
    }

    fun getRecordingsList(): List<File> {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = directory?.listFiles()?.filter { it.extension == "3gp" || it.extension == "aac" } ?: emptyList()
        Log.d("AudioRecorderRepository", "Recordings list retrieved: ${files.size} files")
        return files
    }
}
