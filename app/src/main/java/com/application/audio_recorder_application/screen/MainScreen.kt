package com.application.audio_recorder_application.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.application.audio_recorder_application.viewmodel.AudioViewModel

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "recorder",
            Modifier.padding(paddingValues)
        ) {
            composable("recorder") {
                val viewModel: AudioViewModel = hiltViewModel()
                RecorderScreen(viewModel = viewModel)
            }
            composable("player") {
                // Получаем экземпляр AudioViewModel с помощью hiltViewModel и передаем его в PlayerScreen
                val viewModel: AudioViewModel = hiltViewModel()
                PlayerScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    BottomNavigation(
        backgroundColor = Color.Red,
        contentColor = Color.White
    ) {
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Mic, contentDescription = "Recorder") },
            label = { Text("Recorder") },
            selected = false,
            onClick = { navController.navigate("recorder") }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Player") },
            label = { Text("Player") },
            selected = false,
            onClick = { navController.navigate("player") }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = false,
            onClick = { navController.navigate("settings") }
        )
    }
}
