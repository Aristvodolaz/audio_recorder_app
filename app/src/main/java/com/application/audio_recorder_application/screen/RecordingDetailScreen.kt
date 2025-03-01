package com.application.audio_recorder_application.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.application.audio_recorder_application.data.RecordingRepository
import com.application.audio_recorder_application.data.model.Recording
import com.application.audio_recorder_application.util.EmotionRecognitionService
import com.application.audio_recorder_application.viewmodel.RecordingViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    recordingId: String,
    viewModel: RecordingViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsState()
    val recording = recordings.find { it.id == recordingId }
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val emotionAnalysisResult by viewModel.emotionAnalysisResult.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var showEffectsDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showTrimDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadRecordings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recording?.fileName ?: "Запись") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (recording == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Запись не найдена")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Информация о записи
                RecordingInfoCard(recording)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Визуализация волны
                WaveformCard(recording)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Кнопки управления
                ActionButtonsRow(
                    onPlayClick = { /* Воспроизведение будет добавлено позже */ },
                    onTranscribeClick = { 
                        coroutineScope.launch {
                            viewModel.transcribeRecording(recording)
                        }
                    },
                    onEffectsClick = { showEffectsDialog = true },
                    onEncryptClick = {
                        coroutineScope.launch {
                            viewModel.toggleEncryption(recording)
                        }
                    },
                    isEncrypted = recording.isEncrypted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Кнопка анализа эмоций
                EmotionAnalysisButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.analyzeEmotions(recording)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Результаты анализа эмоций
                emotionAnalysisResult?.let { result ->
                    EmotionAnalysisResultCard(result)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Дополнительные действия
                AdditionalActionsCard(
                    onTrimClick = { showTrimDialog = true },
                    onFormatClick = { showFormatDialog = true },
                    onNotesClick = { showNotesDialog = true },
                    onCategoryClick = { showCategoryDialog = true },
                    onTagsClick = { showTagsDialog = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Транскрипция
                if (!recording.transcription.isNullOrEmpty()) {
                    TranscriptionCard(recording.transcription)
                }
                
                // Заметки
                if (!recording.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NotesCard(recording.notes)
                }
                
                // Диалоги
                if (showEffectsDialog) {
                    AudioEffectsDialog(
                        onDismiss = { showEffectsDialog = false },
                        onEffectSelected = { effect ->
                            coroutineScope.launch {
                                viewModel.applyAudioEffect(recording, effect)
                                showEffectsDialog = false
                            }
                        }
                    )
                }
                
                if (showFormatDialog) {
                    FormatConversionDialog(
                        onDismiss = { showFormatDialog = false },
                        onFormatSelected = { format ->
                            coroutineScope.launch {
                                viewModel.convertFormat(recording, format)
                                showFormatDialog = false
                            }
                        }
                    )
                }
                
                if (showTrimDialog) {
                    TrimAudioDialog(
                        recording = recording,
                        onDismiss = { showTrimDialog = false },
                        onTrimConfirmed = { start, duration ->
                            coroutineScope.launch {
                                viewModel.trimAudio(recording, start, duration)
                                showTrimDialog = false
                            }
                        }
                    )
                }
                
                if (showNotesDialog) {
                    NotesEditDialog(
                        initialNotes = recording.notes ?: "",
                        onDismiss = { showNotesDialog = false },
                        onSave = { notes ->
                            coroutineScope.launch {
                                viewModel.addNote(recording, notes)
                                showNotesDialog = false
                            }
                        }
                    )
                }
            }
            
            // Индикатор загрузки
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            
            // Отображение ошибки
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("ОК")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun RecordingInfoCard(recording: Recording) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(recording.dateCreated)
    val durationFormatted = formatDuration(recording.duration)
    val sizeFormatted = formatSize(recording.size)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = recording.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = durationFormatted,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sizeFormatted,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = recording.format.uppercase(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Категория: ${recording.category}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (recording.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Теги: ${recording.tags.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WaveformCard(recording: Recording) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Если есть данные для визуализации волны, отображаем их
            if (recording.waveformData != null) {
                // Здесь должна быть визуализация волны
                Text(
                    text = "Визуализация волны",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Визуализация недоступна",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onPlayClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    onEffectsClick: () -> Unit,
    onEncryptClick: () -> Unit,
    isEncrypted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.Default.PlayArrow,
            label = "Воспроизвести",
            onClick = onPlayClick
        )
        
        ActionButton(
            icon = Icons.Default.Mic,
            label = "Распознать",
            onClick = onTranscribeClick
        )
        
        ActionButton(
            icon = Icons.Default.Tune,
            label = "Эффекты",
            onClick = onEffectsClick
        )
        
        ActionButton(
            icon = if (isEncrypted) Icons.Default.LockOpen else Icons.Default.Lock,
            label = if (isEncrypted) "Расшифровать" else "Зашифровать",
            onClick = onEncryptClick
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(percent = 50)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AdditionalActionsCard(
    onTrimClick: () -> Unit,
    onFormatClick: () -> Unit,
    onNotesClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onTagsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Дополнительные действия",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Обрезать",
                    onClick = onTrimClick
                )

                ActionButton(
                    icon = Icons.Default.Transform,
                    label = "Конвертировать",
                    onClick = onFormatClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionButton(
                    icon = Icons.Default.Edit,
                    label = "Заметки",
                    onClick = onNotesClick
                )

                ActionButton(
                    icon = Icons.Default.Category,
                    label = "Категория",
                    onClick = onCategoryClick
                )

                ActionButton(
                    icon = Icons.Default.Tag,
                    label = "Теги",
                    onClick = onTagsClick
                )
            }
        }
    }
}

@Composable
fun TranscriptionCard(transcription: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Транскрипция",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = transcription,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Заметки",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AudioEffectsDialog(
    onDismiss: () -> Unit,
    onEffectSelected: (RecordingRepository.AudioEffectType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите эффект") },
        text = {
            Column {
                EffectItem("Робот", RecordingRepository.AudioEffectType.ROBOT, onEffectSelected)
                EffectItem("Бурундук", RecordingRepository.AudioEffectType.CHIPMUNK, onEffectSelected)
                EffectItem("Глубокий голос", RecordingRepository.AudioEffectType.DEEP_VOICE, onEffectSelected)
                EffectItem("Удалить тишину", RecordingRepository.AudioEffectType.REMOVE_SILENCE, onEffectSelected)
                EffectItem("Улучшить звук", RecordingRepository.AudioEffectType.ENHANCE, onEffectSelected)
                EffectItem("Нормализовать громкость", RecordingRepository.AudioEffectType.NORMALIZE, onEffectSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EffectItem(
    name: String,
    effect: RecordingRepository.AudioEffectType,
    onEffectSelected: (RecordingRepository.AudioEffectType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onEffectSelected(effect) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun FormatConversionDialog(
    onDismiss: () -> Unit,
    onFormatSelected: (String) -> Unit
) {
    val formats = listOf("mp3", "wav", "aac", "ogg", "flac")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите формат") },
        text = {
            Column {
                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onFormatSelected(format) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = format.uppercase(),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimAudioDialog(
    recording: Recording,
    onDismiss: () -> Unit,
    onTrimConfirmed: (Int, Int) -> Unit
) {
    var startTime by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf((recording.duration / 1000).toInt()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обрезать аудио") },
        text = {
            Column {
                Text("Начало (секунды):")
                Slider(
                    value = startTime.toFloat(),
                    onValueChange = { startTime = it.toInt() },
                    valueRange = 0f..(recording.duration / 1000).toFloat(),
                    steps = ((recording.duration / 1000) / 5).toInt()
                )
                Text("$startTime сек.")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Длительность (секунды):")
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 1f..((recording.duration / 1000) - startTime).toFloat(),
                    steps = ((recording.duration / 1000) / 5).toInt()
                )
                Text("$duration сек.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onTrimConfirmed(startTime, duration) }) {
                Text("Обрезать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заметки") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("Введите заметки...") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EmotionAnalysisButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Анализировать эмоции",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmotionAnalysisResultCard(result: EmotionRecognitionService.EmotionAnalysisResult) {
    val emotionService = LocalContext.current.applicationContext
        .getSystemService("emotionRecognitionService") as? EmotionRecognitionService
        ?: return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Анализ эмоций",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Основная эмоция
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emotionService.getEmotionEmoji(result.primaryEmotion),
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Основная эмоция: ${result.primaryEmotion.name.lowercase().capitalize()}",
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Уверенность: ${(result.confidence * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Вторичная эмоция, если есть
            result.secondaryEmotion?.let { secondaryEmotion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = emotionService.getEmotionEmoji(secondaryEmotion),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Вторичная эмоция: ${secondaryEmotion.name.lowercase().capitalize()}",
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "Уверенность: ${(result.secondaryConfidence * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Визуализация распределения эмоций
            EmotionDistributionChart(result.emotionMap)
        }
    }
}

@Composable
fun EmotionDistributionChart(emotionMap: Map<EmotionRecognitionService.Emotion, Float>) {
    val emotionService = LocalContext.current.applicationContext
        .getSystemService("emotionRecognitionService") as? EmotionRecognitionService
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Распределение эмоций",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        emotionMap.entries.sortedByDescending { it.value }.forEach { (emotion, value) ->
            val color = emotionService?.getEmotionColor(emotion)?.let { Color(it) }
                ?: MaterialTheme.colorScheme.primary
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = emotion.name.lowercase().capitalize(),
                    modifier = Modifier.width(100.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(value)
                            .background(color)
                    )
                }
                
                Text(
                    text = "${(value * 100).toInt()}%",
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// Расширение для капитализации первой буквы строки
fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
} 