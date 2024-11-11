
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import kotlin.random.Random
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

@Composable
fun RecorderScreen(viewModel: AudioViewModel = hiltViewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    val context = LocalContext.current
    var seconds by remember { mutableStateOf(0) } // Отсчет времени в секундах
    val amplitude by viewModel.amplitude.collectAsState()

    // Лаунчер для запроса разрешения
    val permissionGranted = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            permissionGranted.value = isGranted
        }
    )

    // Проверяем разрешение при запуске
    LaunchedEffect(Unit) {
        permissionGranted.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Запускаем таймер, когда запись активна
    LaunchedEffect(isRecording) {
        if (isRecording) {
            seconds = 0 // Сбрасываем таймер при начале записи
            while (isRecording) {
                delay(1000L) // Задержка 1 секунда
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
        // Отображаем таймер записи
        Text(
            text = formatTime(seconds),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка записи с иконкой микрофона
        IconButton(
            onClick = {
                // Проверяем, есть ли разрешение перед началом записи
                if (!permissionGranted.value) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        val filePath = "${context.filesDir.absolutePath}/recording_${System.currentTimeMillis()}.3gp"
                        viewModel.startRecording(filePath)
                    }
                }
            },
            modifier = Modifier
                .size(100.dp)
                .background(if (isRecording) Color.Gray else Color.Red, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Record",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Информация о записи
        Text(
            text = "AAC (m4a) 16 kHz 128 kbps Mono",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Визуализация звуковой волны
        SoundWaveVisualizer(isRecording = isRecording, amplitude = amplitude)
    }
}

// Форматирует время в формате "ЧЧ:ММ:СС"
fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

@Composable
fun SoundWaveVisualizer(isRecording: Boolean, amplitude: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val waveWidth = size.width / 25 // Ширина каждой полоски
            val normalizedAmplitude = (amplitude / 32768f).coerceIn(0f, 1f) // Нормализация амплитуды

            for (i in 0 until 25) {
                val x = i * waveWidth
                val heightFactor = if (isRecording) normalizedAmplitude * (0.5f + Random.nextFloat() * 0.5f) else 0.3f
                val currentHeight = size.height * heightFactor

                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color.Green, Color.Green.copy(alpha = 0.4f))
                )

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
}
