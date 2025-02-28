package com.application.audio_recorder_application.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SoundWaveDarkPrimary,
    secondary = SoundWaveDarkSecondary,
    tertiary = SoundWaveAccent,
    background = SoundWaveDarkBackground,
    surface = SoundWaveDarkSurface,
    error = SoundWaveDarkError,
    onPrimary = SoundWaveLightSurface,
    onSecondary = SoundWaveDarkBackground,
    onTertiary = SoundWaveDarkBackground,
    onBackground = SoundWaveLightSurface,
    onSurface = SoundWaveLightSurface,
    onError = SoundWaveDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = SoundWaveLightPrimary,
    secondary = SoundWaveLightSecondary,
    tertiary = SoundWaveAccent,
    background = SoundWaveLightBackground,
    surface = SoundWaveLightSurface,
    error = SoundWaveLightError,
    onPrimary = SoundWaveLightSurface,
    onSecondary = SoundWaveDarkBackground,
    onTertiary = SoundWaveDarkBackground,
    onBackground = SoundWaveDarkBackground,
    onSurface = SoundWaveDarkBackground,
    onError = SoundWaveLightSurface
)

@Composable
fun SoundWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Отключаем динамические цвета по умолчанию для сохранения брендинга
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Альтернативное имя для обратной совместимости
@Composable
fun Audio_recorder_applicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SoundWaveTheme(darkTheme, dynamicColor, content)
}