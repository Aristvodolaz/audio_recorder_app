package com.application.audio_recorder_application.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.application.audio_recorder_application.ui.theme.StatusPaused
import com.application.audio_recorder_application.ui.theme.StatusRecording
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    navController: NavHostController,
    viewModel: AudioViewModel = hiltViewModel()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val seconds by viewModel.seconds.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Инициализация запроса разрешения на запись аудио
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.permissionGranted.value = isGranted
            if (isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar("Разрешение на запись получено")
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Для записи необходимо разрешение на использование микрофона")
                }
            }
        }
    )
    
    // Устанавливаем launcher в ViewModel
    LaunchedEffect(Unit) {
        viewModel.permissionLauncher = permissionLauncher
        
        // Проверяем текущий статус разрешения
        viewModel.permissionGranted.value = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Состояние для хранения информации о доступном месте
    var availableStorage by remember { mutableStateOf("") }
    var currentFileSize by remember { mutableStateOf("") }
    
    // Периодическое обновление информации о хранилище
    LaunchedEffect(isRecording) {
        while (true) {
            if (isRecording) {
                // Обновляем информацию о доступном месте
                val availableBytes = viewModel.getAvailableStorage()
                availableStorage = formatFileSize(availableBytes)
                
                // Обновляем информацию о текущем размере файла
                val currentBytes = viewModel.getCurrentFileSize()
                currentFileSize = formatFileSize(currentBytes)
            }
            delay(1000) // Обновляем каждую секунду
        }
    }
    
    // Обработка сообщений Snackbar
    LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let { snackbarHostState.showSnackbar(it) }
            viewModel.clearSnackbarMessage()

    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Запись аудио",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточка с информацией о записи
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Отображение времени записи
                    Text(
                        text = formatTime(seconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    // Статус записи
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusText = when {
                            isRecording && !isPaused -> "Запись"
                            isPaused -> "Пауза"
                            else -> "Готов к записи"
                        }
                        
                        val statusColor = when {
                            isRecording && !isPaused -> StatusRecording
                            isPaused -> StatusPaused
                            else -> MaterialTheme.colorScheme.primary
                        }
                        
                        val statusIcon = when {
                            isRecording && !isPaused -> Icons.Filled.Mic
                            isPaused -> Icons.Filled.Pause
                            else -> Icons.Filled.MicNone
                        }
                        
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor
                        )
                    }
                    
                    // Информация о хранилище
                    if (isRecording) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem(
                                icon = Icons.Filled.Storage,
                                label = "Доступно",
                                value = availableStorage
                            )
                            
                            InfoItem(
                                icon = Icons.Filled.DataUsage,
                                label = "Размер файла",
                                value = currentFileSize
                            )
                        }
                    }
                }
            }
            
            // Визуализация звуковой волны
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording && !isPaused) {
                    WaveformVisualizer(amplitude = amplitude)
                } else if (isPaused) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = null,
                            tint = StatusPaused,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Запись приостановлена",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Waves,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите кнопку записи, чтобы начать",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Кнопки управления записью
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка остановки записи
                if (isRecording) {
                    RecordingButton(
                        icon = Icons.Filled.Stop,
                        description = "Остановить запись",
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { viewModel.completeRecording() }
                    )
                }
                
                // Кнопка записи/паузы
                RecordingButton(
                    icon = when {
                        isRecording && !isPaused -> Icons.Filled.Pause
                        isPaused -> Icons.Filled.PlayArrow
                        else -> Icons.Filled.Mic
                    },
                    description = when {
                        isRecording && !isPaused -> "Приостановить запись"
                        isPaused -> "Продолжить запись"
                        else -> "Начать запись"
                    },
                    backgroundColor = when {
                        isRecording && !isPaused -> StatusRecording
                        isPaused -> StatusPaused
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = MaterialTheme.colorScheme.surface,
                    size = 72.dp,
                    onClick = {
                        when {
                            isRecording && !isPaused -> viewModel.pauseRecording()
                            isPaused -> viewModel.resumeRecording()
                            else -> viewModel.startRecording()
                        }
                    }
                )
                
                // Кнопка отмены записи
                if (isRecording) {
                    RecordingButton(
                        icon = Icons.Filled.Delete,
                        description = "Отменить запись",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { viewModel.cancelRecording() }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecordingButton(
    icon: ImageVector,
    description: String,
    backgroundColor: Color,
    contentColor: Color,
    size: Dp = 56.dp,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(icon) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(100)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(100)
        )
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale.value)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier.size(size / 2)
        )
    }
}

@Composable
fun WaveformVisualizer(amplitude: Int) {
    val maxBars = 50
    val barWidth = 4.dp
    val barSpacing = 2.dp
    val maxAmplitude = 32767 // Максимальное значение амплитуды
    
    val normalizedAmplitude = (amplitude.toFloat() / maxAmplitude).coerceIn(0f, 1f)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxBars) { index ->
            val barHeight = remember { Animatable(0f) }
            
            // Генерируем высоту для каждой полосы на основе амплитуды и случайного фактора
            val targetHeight = normalizedAmplitude * 
                (0.3f + 0.7f * (1f - abs(index - maxBars / 2) / (maxBars / 2f))) * 
                (0.5f + 0.5f * Random.nextFloat())
            
            LaunchedEffect(normalizedAmplitude) {
                barHeight.animateTo(
                    targetValue = targetHeight,
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            
            // Используем градиент от светло-бирюзового к темно-синему для волны
            val waveColor = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = barSpacing / 2)
                    .width(barWidth)
                    .height(100.dp * barHeight.value)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(waveColor)
            )
        }
    }
}

// Функция для форматирования времени
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// Функция для форматирования размера файла
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
