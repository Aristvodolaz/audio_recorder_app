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

    private val _isPausedFlow = MutableStateFlow(false)
    val isPausedFlow = _isPausedFlow.asStateFlow()

    private var isPaused = false
    private var currentFilePath: String? = null

    val isRecording: Boolean
        get() = _isRecordingFlow.value

    fun startRecording(filePath: String) {
        if (!checkStorageSpace()) {
            Log.e("AudioRecorderRepository", "Недостаточно места для записи")
            return
        }
        
        // Ensure any existing recording is stopped
        if (isRecording) {
            try {
                stopRecording()
            } catch (e: Exception) {
                Log.e("AudioRecorderRepository", "Error stopping previous recording: ${e.message}")
            }
        }
        
        try {
            // Create directory if it doesn't exist
            val file = File(filePath)
            file.parentFile?.mkdirs()

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(192000)
                setOutputFile(filePath)
                prepare()
                start()
            }
            
            _isRecordingFlow.value = true
            _isPausedFlow.value = false
            currentFilePath = filePath

            Log.d("AudioRecorderRepository", "Recording started with format: MPEG_4, sample rate: 44100Hz, bitrate: 192000bps, file: $filePath")
        } catch (e: Exception) {
            Log.e("AudioRecorderRepository", "Error starting recording: ${e.message}")
            releaseRecorder()
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

    fun seekTo(position: Long) {
        try {
            mediaPlayer?.seekTo(position.toInt())
            _currentPlaybackPosition.value = position.toInt()
            Log.d("AudioRecorderRepository", "Seeked to position: $position ms")
        } catch (e: Exception) {
            Log.e("AudioRecorderRepository", "Error seeking: ${e.message}")
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
        val files = directory?.listFiles()?.filter {
            it.isFile && (it.extension.equals("3gp", ignoreCase = true) ||
                    it.extension.equals("aac", ignoreCase = true) ||
                    it.extension.equals("m4a", ignoreCase = true) ||
                    it.extension.equals("mp4", ignoreCase = true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        
        Log.d("AudioRecorderRepository", "Recordings list retrieved: ${files.size} files")
        
        // Выведем список всех найденных файлов для отладки
        files.forEachIndexed { index, file ->
            Log.d("AudioRecorderRepository", "Recording $index: ${file.name}, size: ${file.length() / 1024} KB, lastModified: ${java.util.Date(file.lastModified())}")
        }
        
        return files
    }

    fun getCurrentPlaybackPosition(): Long {
        return try {
            (mediaPlayer?.currentPosition ?: 0).toLong()
        } catch (e: Exception) {
            Log.e("AudioRecorderRepository", "Error getting position: ${e.message}")
            0L
        }
    }

    // Метод для проверки доступного места на устройстве
    fun checkStorageSpace(): Boolean {
        val minRequiredSpace = 10 * 1024 * 1024 // 10 МБ минимум
        
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val availableSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val stat = android.os.StatFs(directory?.path)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (e: Exception) {
                Log.e("AudioRecorderRepository", "Error checking storage space: ${e.message}")
                0L
            }
        } else {
            try {
                val stat = android.os.StatFs(directory?.path)
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            } catch (e: Exception) {
                Log.e("AudioRecorderRepository", "Error checking storage space: ${e.message}")
                0L
            }
        }
        
        val hasEnoughSpace = availableSpace > minRequiredSpace
        Log.d("AudioRecorderRepository", "Available storage space: ${availableSpace / (1024 * 1024)} MB, has enough space: $hasEnoughSpace")
        
        return hasEnoughSpace
    }

    // Метод для получения доступного места на устройстве
    fun getAvailableStorage(): Long {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val stat = android.os.StatFs(directory?.path)
                stat.availableBlocksLong * stat.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                val stat = android.os.StatFs(directory?.path)
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderRepository", "Error getting available storage: ${e.message}")
            0L
        }
    }
}
