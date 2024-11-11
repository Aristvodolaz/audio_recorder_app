package com.application.audio_recorder_application.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AudioRecorderScreen(viewModel: AudioViewModel = hiltViewModel()) {
    val transcriptionResult by viewModel.transcriptionResult.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingsList by viewModel.recordingsList.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Таймер записи
    var seconds by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Уровень громкости для визуализации
    val volumeLevel by viewModel.volumeLevel.collectAsState()

    // Лаунчер для запроса разрешения
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startRecording()
                startTimer(coroutineScope) { seconds++ }
            } else {
                viewModel.updateTranscriptionResult("Permission denied")
            }
        }
    )

    // Запрос списка записей при старте экрана
    LaunchedEffect(Unit) {
        viewModel.loadRecordings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Таймер записи
        Text(
            text = "Recording Time: ${secondsToTimeFormat(seconds)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) Color.Red else Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Визуализация громкости
        VolumeVisualizer(volumeLevel = volumeLevel)

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки управления записью
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка начала/остановки записи
            IconButton(
                onClick = {
                    if (activity != null && ContextCompat.checkSelfPermission(
                            activity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (!isRecording) {
                            viewModel.startRecording()
                            startTimer(coroutineScope) { seconds++ }
                        } else {
                            viewModel.stopRecording()
                            coroutineScope.launch { seconds = 0 }  // Сброс таймера
                        }
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = if (isRecording) Color.Red else Color.Green,
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = if (isRecording) "Stop" else "Rec",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Кнопка паузы/возобновления записи
            if (isRecording) {
                IconButton(
                    onClick = {
                        viewModel.togglePauseResume()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (isPaused) Color.Blue else Color.Yellow,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = if (isPaused) "Resume" else "Pause",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Слайдер для регулировки громкости
        Text(text = "Adjust Volume", fontSize = 16.sp)
        Slider(
            value = volumeLevel,
            onValueChange = { level -> viewModel.updateVolumeLevel(level) },
            valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Отображение результата транскрибации
        Text(
            text = "Transcription: $transcriptionResult",
            modifier = Modifier.padding(8.dp),
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Список записанных файлов
        Text(
            text = "Recordings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
        ) {
            items(recordingsList) { file ->
                RecordingItem(file = file, onPlayClick = { viewModel.playRecording(file) }, onDeleteClick = {
                    // Implement delete functionality as needed
                })
            }
        }
    }
}

@Composable
fun VolumeVisualizer(volumeLevel: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(volumeLevel)
                .background(Color.Green, shape = RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun RecordingItem(file: File, onPlayClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = file.name,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPlayClick) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Recording")
        }
        IconButton(onClick = onDeleteClick) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Recording")
        }
    }
}

// Вспомогательная функция для преобразования секунд в формат "MM:SS"
fun secondsToTimeFormat(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

// Запуск таймера для отображения времени записи
fun startTimer(scope: CoroutineScope, onTick: () -> Unit) {
    scope.launch {
        while (true) {
            delay(1000L)
            onTick()
        }
    }
}
