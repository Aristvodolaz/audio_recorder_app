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
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RecorderScreen(viewModel: AudioViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    var seconds by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> permissionGranted.value = isGranted }
    )

    // Таймер записи
    LaunchedEffect(isRecording) {
        if (isRecording && !isPaused) {
            seconds = 0
            while (isRecording && !isPaused) {
                delay(1000L)
                seconds++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    if (!permissionGranted.value) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (isRecording) {
                            if (isPaused) {
                                viewModel.resumeRecording()
                            } else {
                                viewModel.pauseRecording()
                            }
                        } else {
                            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                            val filePath = "${outputDir?.absolutePath}/recording_${System.currentTimeMillis()}.3gp"
                            viewModel.startRecording(filePath)
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
            text = "AAC (m4a) 16 kHz 128 kbps Mono",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Snackbar для уведомлений
        SnackbarHost(hostState = snackbarHostState)
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
