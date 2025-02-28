package com.application.audio_recorder_application.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class SpeechRecognitionService(private val context: Context) {

    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    suspend fun recognizeSpeechFromFile(file: File, language: String = "ru-RU"): String {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, Uri.fromFile(file).toString())
                    }

                    val listener = object : RecognitionListener {
                        val stringBuilder = StringBuilder()

                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудио"
                                SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно разрешений"
                                SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
                                SpeechRecognizer.ERROR_NO_MATCH -> "Нет совпадений"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
                                SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Таймаут речи"
                                else -> "Неизвестная ошибка"
                            }
                            Log.e("SpeechRecognition", "Error: $errorMessage")
                            if (stringBuilder.isEmpty()) {
                                continuation.resume("Не удалось распознать речь: $errorMessage")
                            } else {
                                continuation.resume(stringBuilder.toString())
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                stringBuilder.append(matches[0])
                            }
                            continuation.resume(stringBuilder.toString())
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                stringBuilder.append(matches[0] + " ")
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    }

                    speechRecognizer.setRecognitionListener(listener)
                    speechRecognizer.startListening(recognizerIntent)

                    continuation.invokeOnCancellation {
                        speechRecognizer.cancel()
                    }
                } catch (e: Exception) {
                    Log.e("SpeechRecognition", "Exception: ${e.message}")
                    continuation.resume("Ошибка распознавания: ${e.message}")
                }
            }
        }
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
} 