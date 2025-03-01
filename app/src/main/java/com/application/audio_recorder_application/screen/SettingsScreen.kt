package com.application.audio_recorder_application.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.audio_recorder_application.data.SettingsRepository
import com.application.audio_recorder_application.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val audioSource by viewModel.audioSource.collectAsState()
    val recordingFormat by viewModel.recordingFormat.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()
    val bitrate by viewModel.bitrate.collectAsState()
    val recordingsFolder by viewModel.recordingsFolder.collectAsState()
    val language by viewModel.language.collectAsState()
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSaveMessage by remember { mutableStateOf(false) }
    var showApplyAllDialog by remember { mutableStateOf(false) }
    
    // Эффект для показа сообщения о сохранении настроек
    LaunchedEffect(showSaveMessage) {
        if (showSaveMessage) {
            snackbarHostState.showSnackbar("Настройки сохранены и будут применены при следующей записи")
            showSaveMessage = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showApplyAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Применить все настройки",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Секция настроек записи
            SettingsSection(
                title = "Настройки записи",
                icon = Icons.Default.Mic
            ) {
                // Источник аудио
                DropdownSettingsItem(
                    title = "Источник аудио",
                    selectedValue = audioSource,
                    options = listOf("По умолчанию", "Микрофон", "Камкордер"),
                    icon = Icons.Default.Mic,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.AUDIO_SOURCE_KEY, it)
                        showSaveMessage = true
                    }
                )

                // Формат записи
                DropdownSettingsItem(
                    title = "Формат записи",
                    selectedValue = recordingFormat,
                    options = listOf("AAC (m4a)", "WAV", "MP3"),
                    icon = Icons.Default.AudioFile,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.RECORDING_FORMAT_KEY, it)
                        showSaveMessage = true
                    }
                )

                // Частота дискретизации
                DropdownSettingsItem(
                    title = "Частота дискретизации",
                    selectedValue = sampleRate,
                    options = listOf("8 kHz", "16 kHz", "22.05 kHz", "44.1 kHz", "48 kHz"),
                    icon = Icons.Default.GraphicEq,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.SAMPLE_RATE_KEY, it)
                        showSaveMessage = true
                    }
                )

                // Битрейт
                DropdownSettingsItem(
                    title = "Битрейт",
                    selectedValue = bitrate,
                    options = listOf("64 kbps", "128 kbps", "192 kbps", "256 kbps", "320 kbps"),
                    icon = Icons.Default.Speed,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.BITRATE_KEY, it)
                        showSaveMessage = true
                    }
                )
            }

            // Секция настроек хранения
            SettingsSection(
                title = "Настройки хранения",
                icon = Icons.Default.Storage
            ) {
                // Папка для записей
                TextSettingsItem(
                    title = "Папка для записей",
                    value = recordingsFolder,
                    icon = Icons.Default.Folder,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.RECORDINGS_FOLDER_KEY, it)
                        showSaveMessage = true
                    }
                )
            }

            // Секция настроек интерфейса
            SettingsSection(
                title = "Настройки интерфейса",
                icon = Icons.Default.Palette
            ) {
                // Язык
                DropdownSettingsItem(
                    title = "Язык",
                    selectedValue = language,
                    options = listOf("Русский", "English", "Español", "Français", "Deutsch"),
                    icon = Icons.Default.Language,
                    onSave = { 
                        viewModel.saveSetting(SettingsRepository.LANGUAGE_KEY, it)
                        showSaveMessage = true
                    }
                )
            }
            
            // Кнопка "Применить все настройки"
            Button(
                onClick = { showApplyAllDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Применить все настройки",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Информация о приложении
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "SoundWave",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Версия 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Диалог подтверждения применения всех настроек
        if (showApplyAllDialog) {
            AlertDialog(
                onDismissRequest = { showApplyAllDialog = false },
                title = { Text("Применить настройки") },
                text = { Text("Вы уверены, что хотите применить все настройки? Это может повлиять на текущие записи.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.applyAllSettings()
                            showApplyAllDialog = false
                            showSaveMessage = true
                        }
                    ) {
                        Text("Применить")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showApplyAllDialog = false }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Заголовок секции
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Разделитель
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        // Содержимое секции
        content()
    }
}

@Composable
fun DropdownSettingsItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    icon: ImageVector,
    onSave: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(selectedValue) }
    var showSaveButton by remember { mutableStateOf(false) }
    
    // Обновляем текущее значение при изменении selectedValue
    LaunchedEffect(selectedValue) {
        currentValue = selectedValue
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок и иконка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Выпадающий список
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Выбрать"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                // Невидимая кнопка для открытия выпадающего списка
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                currentValue = option
                                expanded = false
                                showSaveButton = currentValue != selectedValue
                            },
                            leadingIcon = {
                                if (option == currentValue) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            // Кнопка сохранения
            AnimatedVisibility(
                visible = showSaveButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onSave(currentValue)
                            showSaveButton = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun TextSettingsItem(
    title: String,
    value: String,
    icon: ImageVector,
    onSave: (String) -> Unit
) {
    var currentValue by remember { mutableStateOf(value) }
    var showSaveButton by remember { mutableStateOf(false) }
    
    // Обновляем текущее значение при изменении value
    LaunchedEffect(value) {
        currentValue = value
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок и иконка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Текстовое поле
            OutlinedTextField(
                value = currentValue,
                onValueChange = { 
                    currentValue = it
                    showSaveButton = currentValue != value
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        // Открыть диалог выбора папки
                    }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Выбрать папку"
                        )
                    }
                }
            )
            
            // Кнопка сохранения
            AnimatedVisibility(
                visible = showSaveButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onSave(currentValue)
                            showSaveButton = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
