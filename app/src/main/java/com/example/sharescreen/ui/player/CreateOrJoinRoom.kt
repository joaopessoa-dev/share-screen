package com.example.sharescreen.ui.player


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sharescreen.R
import com.example.sharescreen.navigation.Screen

@Composable
fun CreateOrJoinRoom(
    navController: NavController,
    viewModel : CreateOrJoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Column (
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(id = R.string.app_name))
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.roomCode,
            onValueChange = { viewModel.onRoomCodeChange(it) },
            label = { Text(stringResource(R.string.room_code)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Room.route)
                viewModel.joinRoom()

            }
        ) {
            Text(stringResource(R.string.enter))
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Room.route)
                viewModel.createRoom()

            }
        ) {
            Text(stringResource(R.string.create_room))
        }

    }
}