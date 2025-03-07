package com.application.audio_recorder_application.util

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioProcessingService(private val context: Context) {

    // Изменение голоса (эффект "робот")
    suspend fun applyRobotEffect(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"asetrate=44100*0.9,aresample=44100,atempo=1.1\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Изменение голоса (эффект "чиппанк")
    suspend fun applyChipmunkEffect(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"asetrate=44100*1.3,aresample=44100,atempo=0.8\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Изменение голоса (эффект "глубокий голос")
    suspend fun applyDeepVoiceEffect(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"asetrate=44100*0.8,aresample=44100,atempo=1.25\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Удаление тишины
    suspend fun removeSilence(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"silenceremove=1:0:-50dB:1:0:-50dB\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Обрезка аудио
    suspend fun trimAudio(inputFile: File, outputFile: File, startTimeSeconds: Int, durationSeconds: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -ss $startTimeSeconds -t $durationSeconds " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Конвертация в другой формат
    suspend fun convertFormat(inputFile: File, outputFile: File, format: String = "mp3"): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} ${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Улучшение качества звука
    suspend fun enhanceAudio(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"highpass=f=200,lowpass=f=3000,equalizer=f=1000:width_type=h:width=200:g=6\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Нормализация громкости
    suspend fun normalizeVolume(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val command = "-i ${inputFile.absolutePath} -af " +
                    "\"loudnorm=I=-16:TP=-1.5:LRA=11\" " +
                    "${outputFile.absolutePath}"
            executeFFmpegCommand(command)
        }
    }

    // Генерация данных для визуализации волны
    suspend fun generateWaveformData(inputFile: File): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                // Используем более простой подход - извлекаем амплитуды напрямую
                val command = "-i ${inputFile.absolutePath} -ac 1 -ar 8000 -f s16le -acodec pcm_s16le -y ${context.cacheDir}/temp_audio.raw"
                
                if (executeFFmpegCommand(command)) {
                    val rawFile = File(context.cacheDir, "temp_audio.raw")
                    if (rawFile.exists()) {
                        // Читаем сырые аудиоданные
                        val rawData = rawFile.readBytes()
                        
                        // Создаем массив для хранения амплитуд (максимум 1000 точек)
                        val maxPoints = 1000
                        val samplesPerPoint = rawData.size / 2 / maxPoints
                        val result = ByteArray(maxPoints)
                        
                        // Обрабатываем данные для получения амплитуд
                        for (i in 0 until maxPoints) {
                            var sum = 0
                            val startIdx = i * samplesPerPoint * 2
                            val endIdx = minOf(startIdx + samplesPerPoint * 2, rawData.size)
                            
                            for (j in startIdx until endIdx step 2) {
                                if (j + 1 < rawData.size) {
                                    // Преобразуем два байта в одно 16-битное значение
                                    val sample = (rawData[j + 1].toInt() shl 8) or (rawData[j].toInt() and 0xFF)
                                    sum += Math.abs(sample)
                                }
                            }
                            
                            // Нормализуем и сохраняем значение
                            val avg = if (samplesPerPoint > 0) sum / samplesPerPoint else 0
                            result[i] = (avg / 256).toByte()
                        }
                        
                        // Удаляем временный файл
                        rawFile.delete()
                        
                        return@withContext result
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("AudioProcessingService", "Error generating waveform data: ${e.message}")
                null
            }
        }
    }

    // Объединение аудиофайлов
    suspend fun mergeAudioFiles(inputFiles: List<File>, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            val tempListFile = File(context.cacheDir, "filelist.txt")
            tempListFile.bufferedWriter().use { writer ->
                inputFiles.forEach { file ->
                    writer.write("file '${file.absolutePath}'\n")
                }
            }
            
            val command = "-f concat -safe 0 -i ${tempListFile.absolutePath} -c copy ${outputFile.absolutePath}"
            val result = executeFFmpegCommand(command)
            tempListFile.delete()
            result
        }
    }

    private suspend fun executeFFmpegCommand(command: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AudioProcessingService", "Executing FFmpeg command: $command")
                val session = FFmpegKit.execute(command)
                val returnCode = session.returnCode
                
                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d("AudioProcessingService", "FFmpeg command executed successfully")
                    true
                } else {
                    Log.e("AudioProcessingService", "FFmpeg command failed: ${session.failStackTrace}")
                    false
                }
            } catch (e: Exception) {
                Log.e("AudioProcessingService", "Error executing FFmpeg command: ${e.message}")
                false
            }
        }
    }
} 