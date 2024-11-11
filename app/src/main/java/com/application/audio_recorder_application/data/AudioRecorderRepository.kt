package com.application.audio_recorder_application.data

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

class AudioRecorderRepository @Inject constructor(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow = _isRecordingFlow.asStateFlow()

    private var isPaused = false

    fun startRecording(outputFilePath: String) {
        stopRecording() // Остановить предыдущую запись, если она активна

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
            _isRecordingFlow.value = true
            isPaused = false
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecordingFlow.value = false
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                _isRecordingFlow.value = false // Обновляем состояние, чтобы показать, что запись приостановлена
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                _isRecordingFlow.value = true // Обновляем состояние, чтобы показать, что запись возобновлена
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isPaused = false
            _isRecordingFlow.value = false // Обновляем состояние, чтобы показать, что запись остановлена
        }
    }

    fun playRecording(file: File) {
        stopPlayback() // Остановить текущее воспроизведение, если оно активно

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    /**
     * Удаляет указанный файл записи, если он существует
     */
    fun deleteRecording(file: File): Boolean {
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Возвращает список всех записанных файлов
     */
    fun getRecordingsList(): List<File> {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return directory?.listFiles()?.filter { it.extension == "3gp" } ?: emptyList()
    }
}
