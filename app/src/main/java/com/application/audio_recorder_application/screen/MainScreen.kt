package com.application.audio_recorder_application.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.application.audio_recorder_application.ui.theme.SoundWaveTheme
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import com.application.audio_recorder_application.viewmodel.RecordingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

import androidx.compose.ui.unit.dp
// Определение навигационных элементов
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Recorder : Screen("recorder", "Запись", Icons.Filled.Mic, Icons.Outlined.Mic)
    object Player : Screen("player", "Плеер", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow)
    object Recordings : Screen("recordings", "Записи", Icons.Filled.List, Icons.Outlined.List)
    object Settings : Screen("settings", "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val items = listOf(
    Screen.Recorder,
    Screen.Player,
    Screen.Recordings,
    Screen.Settings
)
@Composable
fun MainScreen() {
    SoundWaveTheme {
        val navController = rememberNavController()
        var showSplash by remember { mutableStateOf(true) }

        // Эффект запуска для отображения сплеш-экрана
        LaunchedEffect(key1 = true) {
            delay(1500)
            showSplash = false
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !showSplash,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                MainContent(navController = navController)
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                SplashScreen()
            }
        }
    }
}

@Composable
fun MainContent(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recorder.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route = Screen.Recorder.route) {
                val viewModel: AudioViewModel = hiltViewModel()
                RecorderScreen(navController = navController, viewModel = viewModel)
            }
            composable(route = Screen.Player.route) {
                val viewModel: AudioViewModel = hiltViewModel()
                PlayerScreen(viewModel = viewModel)
            }
            composable(route = Screen.Recordings.route) {
                val viewModel: RecordingViewModel = hiltViewModel()
                RecordingsScreen(
                    viewModel = viewModel,
                    onRecordingClick = { recording ->
                        navController.navigate("recording_detail/${recording.id}")
                    }
                )
            }
            composable(route = "recording_detail/{recordingId}") { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
                val viewModel: RecordingViewModel = hiltViewModel()
                RecordingDetailScreen(
                    recordingId = recordingId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(route = Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { 
                    Text(
                        text = screen.title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    navController.navigate(screen.route) {
                        // Избегаем создания нескольких копий одного и того же экрана в стеке
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Избегаем повторного создания экрана при повторном выборе
                        launchSingleTop = true
                        // Восстанавливаем состояние при возврате к ранее выбранному экрану
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Анимированный логотип с морской тематикой
            Icon(
                imageVector = Icons.Filled.Waves,
                contentDescription = "SoundWave Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Название приложения
            Text(
                text = "SoundWave",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // Подзаголовок
            Text(
                text = "Запись и воспроизведение аудио",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
