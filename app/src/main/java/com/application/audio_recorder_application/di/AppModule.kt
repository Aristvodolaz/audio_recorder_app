package com.application.audio_recorder_application.di

import android.content.Context
import com.application.audio_recorder_application.data.AudioRecorderRepository
import com.application.audio_recorder_application.data.RecordingRepository
import com.application.audio_recorder_application.data.SettingsRepository
import com.application.audio_recorder_application.data.db.AppDatabase
import com.application.audio_recorder_application.data.db.RecordingDao
import com.application.audio_recorder_application.util.AudioProcessingService
import com.application.audio_recorder_application.util.EmotionRecognitionService
import com.application.audio_recorder_application.util.EncryptionService
import com.application.audio_recorder_application.util.SpeechRecognitionHelper
import com.application.audio_recorder_application.util.SpeechRecognitionService
import com.application.audio_recorder_application.util.WaveformVisualizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioRecorderRepository(
        @ApplicationContext context: Context
    ): AudioRecorderRepository {
        return AudioRecorderRepository(context)
    }

    @Provides
    @Singleton
    fun provideRecordingRepository(
        @ApplicationContext context: Context,
        recordingDao: RecordingDao,
        audioProcessingService: AudioProcessingService,
        encryptionService: EncryptionService,
        speechRecognitionService: SpeechRecognitionService,
        waveformVisualizer: WaveformVisualizer
    ): RecordingRepository {
        return RecordingRepository(
            context,
            recordingDao,
            audioProcessingService,
            encryptionService,
            speechRecognitionService,
            waveformVisualizer
        )
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideSpeechRecognitionHelper(
        @ApplicationContext context: Context
    ): SpeechRecognitionHelper {
        return SpeechRecognitionHelper(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideRecordingDao(appDatabase: AppDatabase): RecordingDao {
        return appDatabase.recordingDao()
    }

    @Provides
    @Singleton
    fun provideAudioProcessingService(@ApplicationContext context: Context): AudioProcessingService {
        return AudioProcessingService(context)
    }

    @Provides
    @Singleton
    fun provideEncryptionService(@ApplicationContext context: Context): EncryptionService {
        return EncryptionService(context)
    }

    @Provides
    @Singleton
    fun provideSpeechRecognitionService(@ApplicationContext context: Context): SpeechRecognitionService {
        return SpeechRecognitionService(context)
    }

    @Provides
    @Singleton
    fun provideWaveformVisualizer(@ApplicationContext context: Context): WaveformVisualizer {
        return WaveformVisualizer(context)
    }
    
    @Provides
    @Singleton
    fun provideEmotionRecognitionService(
        @ApplicationContext context: Context,
        speechRecognitionService: SpeechRecognitionService
    ): EmotionRecognitionService {
        return EmotionRecognitionService(context, speechRecognitionService)
    }
}
