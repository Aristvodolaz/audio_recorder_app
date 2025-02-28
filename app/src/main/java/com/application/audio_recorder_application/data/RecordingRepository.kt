package com.application.audio_recorder_application.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import com.application.audio_recorder_application.data.db.RecordingDao
import com.application.audio_recorder_application.data.model.Recording
import com.application.audio_recorder_application.util.AudioProcessingService
import com.application.audio_recorder_application.util.EncryptionService
import com.application.audio_recorder_application.util.SpeechRecognitionService
import com.application.audio_recorder_application.util.WaveformVisualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val context: Context,
    private val recordingDao: RecordingDao,
    private val audioProcessingService: AudioProcessingService,
    private val encryptionService: EncryptionService,
    private val speechRecognitionService: SpeechRecognitionService,
    private val waveformVisualizer: WaveformVisualizer
) {
    // Получение всех записей
    fun getAllRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings()
    }

    // Получение записей по категории
    fun getRecordingsByCategory(category: String): Flow<List<Recording>> {
        return recordingDao.getRecordingsByCategory(category)
    }

    // Получение избранных записей
    fun getFavoriteRecordings(): Flow<List<Recording>> {
        return recordingDao.getFavoriteRecordings()
    }

    // Получение всех категорий
    fun getAllCategories(): Flow<List<String>> {
        return recordingDao.getAllCategories()
    }

    // Поиск записей
    fun searchRecordings(query: String): Flow<List<Recording>> {
        return recordingDao.searchRecordings(query)
    }

    // Сохранение новой записи
    suspend fun saveRecording(
        filePath: String,
        fileName: String,
        category: String = "Общее",
        tags: List<String> = emptyList()
    ): Recording {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalArgumentException("Файл не существует: $filePath")
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val size = file.length()
            
            // Генерация данных для визуализации волны
            val waveformData = audioProcessingService.generateWaveformData(file)
            
            val recording = Recording(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                filePath = filePath,
                duration = durationMs,
                size = size,
                dateCreated = Date(),
                category = category,
                tags = tags,
                isEncrypted = false,
                waveformData = waveformData,
                format = file.extension
            )
            
            recordingDao.insertRecording(recording)
            recording
        }
    }

    // Обновление записи
    suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording)
    }

    // Удаление записи
    suspend fun deleteRecording(recording: Recording) {
        withContext(Dispatchers.IO) {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            recordingDao.deleteRecording(recording)
        }
    }

    // Шифрование записи
    suspend fun encryptRecording(recording: Recording): Recording {
        return withContext(Dispatchers.IO) {
            val inputFile = File(recording.filePath)
            val encryptedFile = File(
                inputFile.parent,
                "encrypted_${System.currentTimeMillis()}.enc"
            )
            
            if (encryptionService.encryptFile(inputFile, encryptedFile)) {
                // Удаляем оригинальный файл после успешного шифрования
                inputFile.delete()
                
                val updatedRecording = recording.copy(
                    filePath = encryptedFile.absolutePath,
                    isEncrypted = true
                )
                
                recordingDao.updateRecording(updatedRecording)
                updatedRecording
            } else {
                recording
            }
        }
    }

    // Дешифрование записи
    suspend fun decryptRecording(recording: Recording): Recording {
        return withContext(Dispatchers.IO) {
            if (!recording.isEncrypted) {
                return@withContext recording
            }
            
            val encryptedFile = File(recording.filePath)
            val decryptedFile = File(
                encryptedFile.parent,
                "decrypted_${System.currentTimeMillis()}.${recording.format}"
            )
            
            if (encryptionService.decryptFile(encryptedFile, decryptedFile)) {
                // Удаляем зашифрованный файл после успешного дешифрования
                encryptedFile.delete()
                
                val updatedRecording = recording.copy(
                    filePath = decryptedFile.absolutePath,
                    isEncrypted = false
                )
                
                recordingDao.updateRecording(updatedRecording)
                updatedRecording
            } else {
                recording
            }
        }
    }

    // Распознавание речи
    suspend fun transcribeRecording(recording: Recording): Recording {
        return withContext(Dispatchers.IO) {
            val file = File(recording.filePath)
            if (!file.exists() || recording.isEncrypted) {
                return@withContext recording
            }
            
            val transcription = speechRecognitionService.recognizeSpeechFromFile(file)
            
            val updatedRecording = recording.copy(
                transcription = transcription
            )
            
            recordingDao.updateRecording(updatedRecording)
            updatedRecording
        }
    }

    // Применение эффекта к записи
    suspend fun applyAudioEffect(recording: Recording, effectType: AudioEffectType): Recording {
        return withContext(Dispatchers.IO) {
            val inputFile = File(recording.filePath)
            if (!inputFile.exists() || recording.isEncrypted) {
                return@withContext recording
            }
            
            val outputFile = File(
                inputFile.parent,
                "${effectType.prefix}_${inputFile.name}"
            )
            
            val success = when (effectType) {
                AudioEffectType.ROBOT -> audioProcessingService.applyRobotEffect(inputFile, outputFile)
                AudioEffectType.CHIPMUNK -> audioProcessingService.applyChipmunkEffect(inputFile, outputFile)
                AudioEffectType.DEEP_VOICE -> audioProcessingService.applyDeepVoiceEffect(inputFile, outputFile)
                AudioEffectType.REMOVE_SILENCE -> audioProcessingService.removeSilence(inputFile, outputFile)
                AudioEffectType.ENHANCE -> audioProcessingService.enhanceAudio(inputFile, outputFile)
                AudioEffectType.NORMALIZE -> audioProcessingService.normalizeVolume(inputFile, outputFile)
            }
            
            if (success) {
                // Получаем новую длительность и размер
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val size = outputFile.length()
                
                // Генерация новых данных для визуализации волны
                val waveformData = audioProcessingService.generateWaveformData(outputFile)
                
                val updatedRecording = recording.copy(
                    filePath = outputFile.absolutePath,
                    duration = durationMs,
                    size = size,
                    waveformData = waveformData
                )
                
                recordingDao.updateRecording(updatedRecording)
                updatedRecording
            } else {
                recording
            }
        }
    }

    // Обрезка аудио
    suspend fun trimAudio(recording: Recording, startTimeSeconds: Int, durationSeconds: Int): Recording {
        return withContext(Dispatchers.IO) {
            val inputFile = File(recording.filePath)
            if (!inputFile.exists() || recording.isEncrypted) {
                return@withContext recording
            }
            
            val outputFile = File(
                inputFile.parent,
                "trimmed_${inputFile.name}"
            )
            
            if (audioProcessingService.trimAudio(inputFile, outputFile, startTimeSeconds, durationSeconds)) {
                // Получаем новую длительность и размер
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val size = outputFile.length()
                
                // Генерация новых данных для визуализации волны
                val waveformData = audioProcessingService.generateWaveformData(outputFile)
                
                val updatedRecording = recording.copy(
                    filePath = outputFile.absolutePath,
                    duration = durationMs,
                    size = size,
                    waveformData = waveformData
                )
                
                recordingDao.updateRecording(updatedRecording)
                updatedRecording
            } else {
                recording
            }
        }
    }

    // Конвертация в другой формат
    suspend fun convertFormat(recording: Recording, format: String): Recording {
        return withContext(Dispatchers.IO) {
            val inputFile = File(recording.filePath)
            if (!inputFile.exists() || recording.isEncrypted) {
                return@withContext recording
            }
            
            val outputFile = File(
                inputFile.parent,
                "${inputFile.nameWithoutExtension}.$format"
            )
            
            if (audioProcessingService.convertFormat(inputFile, outputFile)) {
                // Получаем новую длительность и размер
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val size = outputFile.length()
                
                val updatedRecording = recording.copy(
                    filePath = outputFile.absolutePath,
                    format = format,
                    size = size,
                    duration = durationMs
                )
                
                recordingDao.updateRecording(updatedRecording)
                updatedRecording
            } else {
                recording
            }
        }
    }

    // Объединение записей
    suspend fun mergeRecordings(recordings: List<Recording>, outputFileName: String): Recording? {
        return withContext(Dispatchers.IO) {
            if (recordings.isEmpty()) {
                return@withContext null
            }
            
            val inputFiles = recordings.map { File(it.filePath) }
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val outputFile = File(outputDir, outputFileName)
            
            if (audioProcessingService.mergeAudioFiles(inputFiles, outputFile)) {
                // Получаем длительность и размер
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val size = outputFile.length()
                
                // Генерация данных для визуализации волны
                val waveformData = audioProcessingService.generateWaveformData(outputFile)
                
                val recording = Recording(
                    id = UUID.randomUUID().toString(),
                    fileName = outputFileName,
                    filePath = outputFile.absolutePath,
                    duration = durationMs,
                    size = size,
                    dateCreated = Date(),
                    category = "Объединенные",
                    tags = listOf("merged"),
                    waveformData = waveformData,
                    format = outputFile.extension
                )
                
                recordingDao.insertRecording(recording)
                recording
            } else {
                null
            }
        }
    }

    // Переключение статуса "избранное"
    suspend fun toggleFavorite(recording: Recording): Recording {
        val updatedRecording = recording.copy(isFavorite = !recording.isFavorite)
        recordingDao.updateRecording(updatedRecording)
        return updatedRecording
    }

    // Добавление заметки к записи
    suspend fun addNote(recording: Recording, note: String): Recording {
        val updatedRecording = recording.copy(notes = note)
        recordingDao.updateRecording(updatedRecording)
        return updatedRecording
    }

    // Типы аудио эффектов
    enum class AudioEffectType(val prefix: String) {
        ROBOT("robot"),
        CHIPMUNK("chipmunk"),
        DEEP_VOICE("deep"),
        REMOVE_SILENCE("nosilence"),
        ENHANCE("enhanced"),
        NORMALIZE("normalized")
    }
} 