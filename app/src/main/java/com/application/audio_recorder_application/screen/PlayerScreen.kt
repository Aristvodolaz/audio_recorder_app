package com.application.audio_recorder_application.screen

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.application.audio_recorder_application.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PlayerScreen(viewModel: AudioViewModel = hiltViewModel()) {
    val recordingsList by viewModel.recordingsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Now Playing",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )
        PlaybackControlPanel(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recordings",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
fun PlaybackControlPanel(viewModel: AudioViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTime by viewModel.currentPlaybackTime.collectAsState()
    val duration by viewModel.currentPlaybackDuration.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Таймлэпс-полоска
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = formatTime(currentTime), fontSize = 12.sp)
                Text(text = formatTime(duration), fontSize = 12.sp)
            }

            Slider(
                value = if (duration > 0) currentTime / duration.toFloat() else 0f,
                onValueChange = { /* Allow seeking */ },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.previousTrack() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = {
                    if (isPlaying) viewModel.pausePlayback() else viewModel.resumePlayback()
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }
                IconButton(onClick = { viewModel.nextTrack() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

fun getRecordingDuration(file: File): Long {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(file.absolutePath)
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    retriever.release()
    return durationMs
}

@Composable
fun RecordingItem(recording: File, onPlayClick: () -> Unit, onDeleteClick: () -> Unit) {
    val duration = remember { formatTime(getRecordingDuration(recording)) }
    val fileSizeKb = (recording.length() / 1024).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = recording.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Duration: $duration | Size: $fileSizeKb KB",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Row {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = (ms / 1000 / 3600)
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}
