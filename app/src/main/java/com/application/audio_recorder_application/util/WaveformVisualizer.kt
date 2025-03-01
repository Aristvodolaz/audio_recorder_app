package com.application.audio_recorder_application.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.application.audio_recorder_application.ui.theme.WaveformActive
import com.application.audio_recorder_application.ui.theme.WaveformInactive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Singleton
class WaveformVisualizer @Inject constructor(
    private val context: Context
) {
    private var visualizer: Visualizer? = null
    private val captureSize = Visualizer.getCaptureSizeRange()[1]
    
    private val _waveformData = MutableStateFlow<FloatArray?>(null)
    val waveformData: StateFlow<FloatArray?> = _waveformData
    
    private val _fftData = MutableStateFlow<FloatArray?>(null)
    val fftData: StateFlow<FloatArray?> = _fftData
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    fun setupVisualizer(audioSessionId: Int) {
        releaseVisualizer()
        
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = this@WaveformVisualizer.captureSize
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        processWaveform(waveform)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        processFft(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                
                enabled = true
            }
            _isActive.value = true
        } catch (e: Exception) {
            Log.e("WaveformVisualizer", "Error setting up visualizer", e)
            _isActive.value = false
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        val amplitudes = FloatArray(waveform.size) { i ->
            // Преобразуем байты в значения от -1 до 1
            waveform[i].toFloat() / 128f
        }
        _waveformData.value = amplitudes
    }

    private fun processFft(fft: ByteArray) {
        // FFT данные приходят в виде комплексных чисел (реальная и мнимая части)
        // Преобразуем их в амплитуды
        val n = fft.size / 2
        val amplitudes = FloatArray(n)
        
        for (i in 0 until n) {
            val real = fft[2 * i].toFloat()
            val imaginary = fft[2 * i + 1].toFloat()
            
            // Вычисляем магнитуду (амплитуду)
            amplitudes[i] = (real * real + imaginary * imaginary) / (128f * 128f)
        }
        
        _fftData.value = amplitudes
    }

    fun releaseVisualizer() {
        visualizer?.apply {
            enabled = false
            release()
        }
        visualizer = null
        _isActive.value = false
        _waveformData.value = null
        _fftData.value = null
    }

    suspend fun generateWaveformData(file: File): FloatArray? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                
                // Получаем длительность аудио
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                
                // Анализируем аудио файл и создаем данные для визуализации
                val samples = extractSamplesFromFile(file)
                
                // Нормализуем и сжимаем данные до 100 точек
                normalizeAndCompressSamples(samples, 100)
            } catch (e: Exception) {
                Log.e("WaveformVisualizer", "Error generating waveform data", e)
                null
            }
        }
    }

    private fun extractSamplesFromFile(file: File): FloatArray {
        // Здесь должен быть код для извлечения аудио сэмплов из файла
        // Это упрощенная реализация, которая создает случайные данные
        // В реальном приложении нужно использовать библиотеки для декодирования аудио
        
        val random = java.util.Random(file.length())
        return FloatArray(100) { 
            (random.nextFloat() * 2 - 1) * 0.8f // Значения от -0.8 до 0.8
        }
    }

    private fun normalizeAndCompressSamples(samples: FloatArray, targetSize: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(targetSize)
        
        // Находим максимальную амплитуду для нормализации
        var maxAmplitude = 0f
        for (sample in samples) {
            maxAmplitude = max(maxAmplitude, abs(sample))
        }
        
        // Если максимальная амплитуда близка к нулю, возвращаем пустой массив
        if (maxAmplitude < 0.01f) return FloatArray(targetSize)
        
        // Нормализуем и сжимаем
        val result = FloatArray(targetSize)
        val samplesPerPoint = samples.size / targetSize
        
        for (i in 0 until targetSize) {
            val startIdx = i * samplesPerPoint
            val endIdx = min((i + 1) * samplesPerPoint, samples.size)
            
            var sum = 0f
            for (j in startIdx until endIdx) {
                sum += abs(samples[j])
            }
            
            // Нормализуем значение
            result[i] = (sum / (endIdx - startIdx)) / maxAmplitude
        }
        
        return result
    }

    fun createWaveformBitmap(
        waveform: FloatArray,
        width: Int,
        height: Int,
        activeColor: Color = WaveformActive,
        inactiveColor: Color = WaveformInactive,
        playbackProgress: Float = 0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val activePaint = Paint().apply {
            color = activeColor.toArgb()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        
        val inactivePaint = Paint().apply {
            color = inactiveColor.toArgb()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        
        val centerY = height / 2f
        val barWidth = width.toFloat() / waveform.size
        val progressX = width * playbackProgress
        
        // Рисуем волну
        val activePath = Path()
        val inactivePath = Path()
        
        for (i in waveform.indices) {
            val x = i * barWidth + barWidth / 2
            val amplitude = waveform[i] * (height / 2f) * 0.8f
            
            val top = centerY - amplitude
            val bottom = centerY + amplitude
            
            val rect = RectF(x - barWidth / 4, top, x + barWidth / 4, bottom)
            
            if (x <= progressX) {
                activePath.addRoundRect(rect, 4f, 4f, Path.Direction.CW)
            } else {
                inactivePath.addRoundRect(rect, 4f, 4f, Path.Direction.CW)
            }
        }
        
        canvas.drawPath(inactivePath, inactivePaint)
        canvas.drawPath(activePath, activePaint)
        
        return bitmap
    }
} 