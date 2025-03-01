package com.application.audio_recorder_application.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

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
    val scaffoldState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Инициализация запроса разрешения на запись аудио
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.permissionGranted.value = isGranted
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
                
                // Если запись идет, обновляем размер текущего файла
                viewModel.currentFilePath.value?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        currentFileSize = formatFileSize(file.length())
                    }
                }
            }
            delay(1000L) // Обновляем каждую секунду
        }
    }
    
    // Показываем сообщение, если оно есть
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scaffoldState.showSnackbar(it)
            // Сбрасываем сообщение после показа
            viewModel.clearSnackbarMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = scaffoldState) },
        topBar = {
            TopAppBar(
                title = { Text("Запись аудио") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Таймер
            Text(
                text = formatTime(seconds),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color.Red else Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Визуализация звука
            SoundWaveVisualizer(isRecording = isRecording, amplitude = amplitude)

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки управления записью
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(
                    onClick = {
                        if (!viewModel.permissionGranted.value) {
                            viewModel.permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isRecording) {
                                if (isPaused) {
                                    viewModel.resumeRecording()
                                    scope.launch {
                                        scaffoldState.showSnackbar("Запись возобновлена")
                                    }
                                } else {
                                    viewModel.pauseRecording()
                                    scope.launch {
                                        scaffoldState.showSnackbar("Запись приостановлена")
                                    }
                                }
                            } else {
                                val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                                // Создаем директорию, если она не существует
                                outputDir?.mkdirs()
                                
                                // Формируем имя файла с текущей датой и временем
                                val timestamp = java.text.SimpleDateFormat(
                                    "yyyyMMdd_HHmmss", 
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())
                                
                                // Используем расширение .m4a вместо .3gp
                                val filePath = "${outputDir?.absolutePath}/REC_${timestamp}.m4a"
                                viewModel.startRecording(filePath)
                                
                                // Показываем сообщение о начале записи
                                scope.launch {
                                    scaffoldState.showSnackbar("Запись началась")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = when {
                                isRecording && isPaused -> Color.Yellow
                                isRecording -> Color.Gray
                                else -> Color.Red
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Record",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isRecording) {
                            viewModel.completeRecording()
                            scope.launch {
                                scaffoldState.showSnackbar("Запись сохранена")
                            }
                        }
                    },
                    enabled = isRecording,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Blue, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Complete",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Дополнительная информация
            Text(
                text = "AAC (m4a) 44.1 kHz 192 kbps Mono",
                fontSize = 16.sp,
                color = Color.Gray
            )

            // Добавляем информацию о доступном месте и размере файла
            if (isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Текущий размер файла: $currentFileSize",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Доступное место: $availableStorage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Формат времени в формате "ЧЧ:ММ:СС"
fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

// Визуализация звуковой волны
@Composable
fun SoundWaveVisualizer(isRecording: Boolean, amplitude: Int) {
    val waveWidthFactor = remember { 25 }
    val normalizedAmplitude = (amplitude / 32768f).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        val waveWidth = size.width / waveWidthFactor
        val gradientBrush = Brush.verticalGradient(colors = listOf(Color.Green, Color.Green.copy(alpha = 0.4f)))

        for (i in 0 until waveWidthFactor) {
            val x = i * waveWidth
            val heightFactor = if (isRecording) normalizedAmplitude * (0.5f + kotlin.random.Random.nextFloat() * 0.5f) else 0.3f
            val currentHeight = size.height * heightFactor

            drawLine(
                brush = gradientBrush,
                start = androidx.compose.ui.geometry.Offset(x, size.height - currentHeight),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = waveWidth / 2,
                cap = StrokeCap.Round
            )
        }
    }
}

// Функция для форматирования размера в удобочитаемый вид
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
