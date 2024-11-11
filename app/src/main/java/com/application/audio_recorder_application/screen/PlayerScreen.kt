package com.application.audio_recorder_application.screen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import java.io.File

@Composable
fun PlayerScreen(viewModel: AudioViewModel = hiltViewModel()) {
    // Получаем список записей из ViewModel
    val recordingsList by viewModel.recordingsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Панель управления воспроизведением
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { /* Предыдущий трек */ }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = { /* Пауза/Воспроизведение */ }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
            IconButton(onClick = { /* Следующий трек */ }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Слайдер для регулировки громкости
        Slider(
            value = 0.5f,
            onValueChange = { /* Изменить громкость */ },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Список записей
        LazyColumn {
            items(recordingsList) { recording ->
                RecordingItem(
                    recording = recording,
                    onPlayClick = { viewModel.playRecording(recording) },
                    onDeleteClick = { viewModel.deleteRecording(recording) }
                )
            }
        }
    }
}

@Composable
fun RecordingItem(recording: File, onPlayClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = recording.name, fontWeight = FontWeight.Bold)
            Text(text = "Duration: 3:24 | Size: ${(recording.length() / 1024)} KB", color = Color.Gray)
        }
        Row {
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
