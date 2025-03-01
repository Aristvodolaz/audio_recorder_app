package com.application.audio_recorder_application.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для распознавания эмоций в речи на основе текстовой транскрипции
 * и аудио характеристик (тон, темп, громкость).
 */
@Singleton
class EmotionRecognitionService @Inject constructor(
    private val context: Context,
    private val speechRecognitionService: SpeechRecognitionService
) {
    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    
    // Возможные эмоции
    enum class Emotion {
        NEUTRAL,
        HAPPY,
        SAD,
        ANGRY,
        EXCITED,
        ANXIOUS,
        UNKNOWN
    }
    
    // Результат анализа эмоций
    data class EmotionAnalysisResult(
        val primaryEmotion: Emotion,
        val confidence: Float,
        val secondaryEmotion: Emotion? = null,
        val secondaryConfidence: Float = 0f,
        val emotionMap: Map<Emotion, Float> = emptyMap()
    )
    
    /**
     * Анализирует эмоции в аудиозаписи
     */
    suspend fun analyzeEmotions(audioFile: File): EmotionAnalysisResult {
        _isProcessing.value = true
        
        return try {
            withContext(Dispatchers.IO) {
                // 1. Получаем транскрипцию аудио
                val transcription = speechRecognitionService.recognizeSpeechFromFile(audioFile)
                
                // 2. Определяем язык текста
                val language = detectLanguage(transcription)
                
                // 3. Анализируем аудио характеристики
                val audioFeatures = extractAudioFeatures(audioFile)
                
                // 4. Анализируем текст на наличие эмоциональных маркеров
                val textEmotions = analyzeTextEmotions(transcription, language)
                
                // 5. Комбинируем результаты анализа текста и аудио
                combineEmotionAnalysis(textEmotions, audioFeatures)
            }
        } catch (e: Exception) {
            Log.e("EmotionRecognition", "Error analyzing emotions", e)
            EmotionAnalysisResult(Emotion.UNKNOWN, 0f)
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Определяет язык текста
     */
    private suspend fun detectLanguage(text: String): String {
        if (text.isBlank()) return "und" // Неопределенный язык
        
        return try {
            val result = languageIdentifier.identifyLanguage(text).await()
            if (result == "und") "ru" else result // Предполагаем русский по умолчанию
        } catch (e: Exception) {
            Log.e("EmotionRecognition", "Error detecting language", e)
            "ru" // Предполагаем русский по умолчанию
        }
    }
    
    /**
     * Извлекает аудио характеристики из файла
     */
    private fun extractAudioFeatures(audioFile: File): Map<String, Float> {
        // В реальном приложении здесь должен быть код для анализа аудио характеристик
        // Например, использование библиотеки TarsosDSP для анализа высоты тона, темпа и т.д.
        
        // Упрощенная реализация для демонстрации
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(audioFile.absolutePath)
        
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
        
        // Генерируем случайные значения для демонстрации
        val random = java.util.Random(audioFile.length())
        
        return mapOf(
            "pitch" to (0.4f + random.nextFloat() * 0.6f), // Высота тона (0.4-1.0)
            "tempo" to (0.5f + random.nextFloat() * 0.5f), // Темп речи (0.5-1.0)
            "volume" to (0.3f + random.nextFloat() * 0.7f), // Громкость (0.3-1.0)
            "variability" to (0.2f + random.nextFloat() * 0.8f) // Вариативность тона (0.2-1.0)
        )
    }
    
    /**
     * Анализирует текст на наличие эмоциональных маркеров
     */
    private fun analyzeTextEmotions(text: String, language: String): Map<Emotion, Float> {
        if (text.isBlank()) return mapOf(Emotion.NEUTRAL to 1.0f)
        
        val emotionScores = mutableMapOf(
            Emotion.NEUTRAL to 0.5f,
            Emotion.HAPPY to 0.0f,
            Emotion.SAD to 0.0f,
            Emotion.ANGRY to 0.0f,
            Emotion.EXCITED to 0.0f,
            Emotion.ANXIOUS to 0.0f
        )
        
        // Словари эмоциональных маркеров
        val happyWords = if (language == "ru") 
            setOf("счастлив", "рад", "отлично", "прекрасно", "замечательно", "супер", "класс", "ура")
        else 
            setOf("happy", "glad", "great", "excellent", "wonderful", "awesome", "amazing", "yay")
            
        val sadWords = if (language == "ru") 
            setOf("грустно", "печально", "жаль", "сожалею", "тоска", "увы", "плохо")
        else 
            setOf("sad", "sorry", "unfortunately", "regret", "miss", "bad", "upset")
            
        val angryWords = if (language == "ru") 
            setOf("злой", "раздражен", "бесит", "ненавижу", "возмущен", "черт")
        else 
            setOf("angry", "annoyed", "hate", "damn", "frustrated", "mad")
            
        val excitedWords = if (language == "ru") 
            setOf("восторг", "невероятно", "потрясающе", "вау", "обалденно", "круто")
        else 
            setOf("excited", "incredible", "wow", "awesome", "cool", "amazing")
            
        val anxiousWords = if (language == "ru") 
            setOf("тревожно", "беспокоюсь", "волнуюсь", "страшно", "опасаюсь", "боюсь")
        else 
            setOf("anxious", "worried", "nervous", "scared", "afraid", "fear")
        
        // Анализируем текст
        val words = text.lowercase().split(Regex("\\s+"))
        
        for (word in words) {
            when {
                happyWords.any { word.contains(it) } -> emotionScores[Emotion.HAPPY] = emotionScores[Emotion.HAPPY]!! + 0.2f
                sadWords.any { word.contains(it) } -> emotionScores[Emotion.SAD] = emotionScores[Emotion.SAD]!! + 0.2f
                angryWords.any { word.contains(it) } -> emotionScores[Emotion.ANGRY] = emotionScores[Emotion.ANGRY]!! + 0.2f
                excitedWords.any { word.contains(it) } -> emotionScores[Emotion.EXCITED] = emotionScores[Emotion.EXCITED]!! + 0.2f
                anxiousWords.any { word.contains(it) } -> emotionScores[Emotion.ANXIOUS] = emotionScores[Emotion.ANXIOUS]!! + 0.2f
            }
        }
        
        // Нормализуем оценки
        val sum = emotionScores.values.sum()
        return emotionScores.mapValues { it.value / sum }
    }
    
    /**
     * Комбинирует результаты анализа текста и аудио
     */
    private fun combineEmotionAnalysis(
        textEmotions: Map<Emotion, Float>,
        audioFeatures: Map<String, Float>
    ): EmotionAnalysisResult {
        // Корректируем оценки эмоций на основе аудио характеристик
        val combinedScores = textEmotions.toMutableMap()
        
        // Высокий тон и громкость часто связаны с радостью или возбуждением
        if (audioFeatures["pitch"] ?: 0f > 0.7f && audioFeatures["volume"] ?: 0f > 0.7f) {
            combinedScores[Emotion.HAPPY] = (combinedScores[Emotion.HAPPY] ?: 0f) + 0.2f
            combinedScores[Emotion.EXCITED] = (combinedScores[Emotion.EXCITED] ?: 0f) + 0.3f
        }
        
        // Низкий тон и громкость часто связаны с грустью
        if (audioFeatures["pitch"] ?: 0f < 0.5f && audioFeatures["volume"] ?: 0f < 0.5f) {
            combinedScores[Emotion.SAD] = (combinedScores[Emotion.SAD] ?: 0f) + 0.3f
        }
        
        // Высокая громкость и вариативность тона могут указывать на гнев
        if (audioFeatures["volume"] ?: 0f > 0.8f && audioFeatures["variability"] ?: 0f > 0.7f) {
            combinedScores[Emotion.ANGRY] = (combinedScores[Emotion.ANGRY] ?: 0f) + 0.3f
        }
        
        // Высокий темп и вариативность могут указывать на возбуждение или тревогу
        if (audioFeatures["tempo"] ?: 0f > 0.7f && audioFeatures["variability"] ?: 0f > 0.6f) {
            combinedScores[Emotion.EXCITED] = (combinedScores[Emotion.EXCITED] ?: 0f) + 0.2f
            combinedScores[Emotion.ANXIOUS] = (combinedScores[Emotion.ANXIOUS] ?: 0f) + 0.2f
        }
        
        // Нормализуем оценки
        val sum = combinedScores.values.sum()
        val normalizedScores = combinedScores.mapValues { it.value / sum }
        
        // Находим основную и вторичную эмоции
        val sortedEmotions = normalizedScores.entries.sortedByDescending { it.value }
        val primaryEmotion = sortedEmotions.firstOrNull()?.key ?: Emotion.NEUTRAL
        val primaryConfidence = sortedEmotions.firstOrNull()?.value ?: 1.0f
        
        val secondaryEmotion = if (sortedEmotions.size > 1) sortedEmotions[1].key else null
        val secondaryConfidence = if (sortedEmotions.size > 1) sortedEmotions[1].value else 0f
        
        return EmotionAnalysisResult(
            primaryEmotion = primaryEmotion,
            confidence = primaryConfidence,
            secondaryEmotion = secondaryEmotion,
            secondaryConfidence = secondaryConfidence,
            emotionMap = normalizedScores
        )
    }
    
    /**
     * Возвращает цвет, соответствующий эмоции
     */
    fun getEmotionColor(emotion: Emotion): Int {
        return when (emotion) {
            Emotion.NEUTRAL -> 0xFF9E9E9E.toInt() // Серый
            Emotion.HAPPY -> 0xFFFFEB3B.toInt() // Желтый
            Emotion.SAD -> 0xFF2196F3.toInt() // Синий
            Emotion.ANGRY -> 0xFFF44336.toInt() // Красный
            Emotion.EXCITED -> 0xFFFF9800.toInt() // Оранжевый
            Emotion.ANXIOUS -> 0xFF9C27B0.toInt() // Фиолетовый
            Emotion.UNKNOWN -> 0xFF757575.toInt() // Темно-серый
        }
    }
    
    /**
     * Возвращает эмодзи, соответствующий эмоции
     */
    fun getEmotionEmoji(emotion: Emotion): String {
        return when (emotion) {
            Emotion.NEUTRAL -> "😐"
            Emotion.HAPPY -> "😊"
            Emotion.SAD -> "😢"
            Emotion.ANGRY -> "😠"
            Emotion.EXCITED -> "😃"
            Emotion.ANXIOUS -> "😰"
            Emotion.UNKNOWN -> "��"
        }
    }
} 