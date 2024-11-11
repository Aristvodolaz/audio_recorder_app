package com.application.audio_recorder_application.screen
import android.media.MediaMetadataRetriever
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
import androidx.compose.runtime.remember
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

fun getRecordingDuration(file: File): String {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(file.absolutePath)
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    retriever.release()

    val hours = (durationMs / 1000) / 3600
    val minutes = ((durationMs / 1000) % 3600) / 60
    val seconds = (durationMs / 1000) % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun RecordingItem(recording: File, onPlayClick: () -> Unit, onDeleteClick: () -> Unit) {
    val duration = remember { getRecordingDuration(recording) }
    val fileSizeKb = (recording.length() / 1024).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = recording.name, fontWeight = FontWeight.Bold)
            Text(text = "Duration: $duration | Size: $fileSizeKb KB", color = Color.Gray)
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
