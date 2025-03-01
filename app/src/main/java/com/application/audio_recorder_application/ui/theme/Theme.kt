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
    onPrimary = SoundWaveDarkBackground,
    primaryContainer = SoundWaveDarkPrimary.copy(alpha = 0.7f),
    onPrimaryContainer = SoundWaveDarkBackground,
    secondary = SoundWaveDarkSecondary,
    onSecondary = SoundWaveDarkBackground,
    secondaryContainer = SoundWaveDarkSecondary.copy(alpha = 0.7f),
    onSecondaryContainer = SoundWaveDarkBackground,
    tertiary = SoundWaveAccent,
    onTertiary = SoundWaveDarkBackground,
    tertiaryContainer = SoundWaveAccent.copy(alpha = 0.7f),
    onTertiaryContainer = SoundWaveDarkBackground,
    background = SoundWaveDarkBackground,
    onBackground = SoundWaveDarkPrimary,
    surface = SoundWaveDarkSurface,
    onSurface = SoundWaveDarkPrimary,
    surfaceVariant = SoundWaveDarkSurface.copy(alpha = 0.7f),
    onSurfaceVariant = SoundWaveDarkPrimary.copy(alpha = 0.7f),
    error = SoundWaveDarkError,
    onError = SoundWaveDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = SoundWaveLightPrimary,
    onPrimary = SoundWaveLightSurface,
    primaryContainer = SoundWaveLightPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = SoundWaveLightPrimary,
    secondary = SoundWaveLightSecondary,
    onSecondary = SoundWaveLightSurface,
    secondaryContainer = SoundWaveLightSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = SoundWaveLightSecondary,
    tertiary = SoundWaveAccent,
    onTertiary = SoundWaveLightSurface,
    tertiaryContainer = SoundWaveAccent.copy(alpha = 0.1f),
    onTertiaryContainer = SoundWaveAccent,
    background = SoundWaveLightBackground,
    onBackground = SoundWaveDarkBackground,
    surface = SoundWaveLightSurface,
    onSurface = SoundWaveDarkBackground,
    surfaceVariant = SoundWaveSurfaceVariant,
    onSurfaceVariant = SoundWaveOnSurfaceVariant,
    error = SoundWaveLightError,
    onError = SoundWaveLightSurface
)

@Composable
fun SoundWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Включаем динамические цвета по умолчанию
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
            // Устанавливаем цвет статус-бара в соответствии с темой
            window.statusBarColor = colorScheme.primary.toArgb()
            // Устанавливаем светлые или темные иконки в зависимости от темы
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Устанавливаем цвет навигационной панели
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    SoundWaveTheme(darkTheme, dynamicColor, content)
}