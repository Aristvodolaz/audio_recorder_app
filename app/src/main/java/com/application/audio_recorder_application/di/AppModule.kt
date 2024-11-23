package com.application.audio_recorder_application.di
import android.content.Context
import com.application.audio_recorder_application.data.AudioRecorderRepository
import com.application.audio_recorder_application.data.SettingsRepository
import com.application.audio_recorder_application.util.SpeechRecognitionHelper
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
}
