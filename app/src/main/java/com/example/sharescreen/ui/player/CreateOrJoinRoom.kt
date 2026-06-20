package com.example.sharescreen.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sharescreen.navigation.Screen

@Composable
fun CreateOrJoinRoom(
    navController: NavController,
    viewModel: CreateOrJoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.navigateToHost) {
        uiState.navigateToHost?.let { event ->
            navController.navigate(Screen.HostRoom.createRoute(event.room.roomCode, event.userName))
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(uiState.navigateToViewer) {
        uiState.navigateToViewer?.let { event ->
            navController.navigate(Screen.ViewerRoom.createRoute(event.room.roomCode, event.userName))
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ShareScreen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.userName,
            onValueChange = viewModel::onUserNameChange,
            label = { Text("Seu nome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(32.dp))


        Text("Transmitir minha tela", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = viewModel::createRoom,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading && uiState.navigateToHost == null) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Criar Sala")
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))


        Text("Assistir transmissão", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.roomCode,
            onValueChange = viewModel::onRoomCodeChange,
            label = { Text("Código da Sala") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = viewModel::joinRoom,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading && uiState.navigateToViewer == null) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Entrar na Sala")
            }
        }

        Spacer(Modifier.height(16.dp))
        SnackbarHost(snackbarHostState)
    }
}
