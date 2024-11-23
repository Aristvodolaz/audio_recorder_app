package com.application.audio_recorder_application.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.audio_recorder_application.data.SettingsRepository
import com.application.audio_recorder_application.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val audioSource by viewModel.audioSource.collectAsState()
    val recordingFormat by viewModel.recordingFormat.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()
    val bitrate by viewModel.bitrate.collectAsState()
    val recordingsFolder by viewModel.recordingsFolder.collectAsState()
    val language by viewModel.language.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .height(56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Настройки с выпадающими списками
            DropdownSettingsItem(
                title = "Audio source",
                selectedValue = audioSource,
                options = listOf("Default", "Microphone", "Camcorder"),
                onSave = { viewModel.saveSetting(SettingsRepository.AUDIO_SOURCE_KEY, it) }
            )

            DropdownSettingsItem(
                title = "Recording format",
                selectedValue = recordingFormat,
                options = listOf("AAC (m4a)", "WAV", "MP3"),
                onSave = { viewModel.saveSetting(SettingsRepository.RECORDING_FORMAT_KEY, it) }
            )

            DropdownSettingsItem(
                title = "Sample rate",
                selectedValue = sampleRate,
                options = listOf("8 kHz", "16 kHz", "44.1 kHz"),
                onSave = { viewModel.saveSetting(SettingsRepository.SAMPLE_RATE_KEY, it) }
            )

            DropdownSettingsItem(
                title = "Encoder bitrate",
                selectedValue = bitrate,
                options = listOf("64 kbps", "128 kbps", "256 kbps"),
                onSave = { viewModel.saveSetting(SettingsRepository.BITRATE_KEY, it) }
            )

            SettingsItem(
                title = "Recordings folder",
                value = recordingsFolder,
                onSave = { viewModel.saveSetting(SettingsRepository.RECORDINGS_FOLDER_KEY, it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingsItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onSave: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(selectedValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentValue,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(), // This attaches the dropdown to the TextField
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                label = { Text("Select $title") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            currentValue = option
                            expanded = false
                            onSave(option) // Сохраняем выбранное значение
                        }
                    )
                }
            }
        }

    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(title: String, value: String, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter $title") },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Button(
            onClick = { onSave(text) },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text("Save")
        }
    }
}
