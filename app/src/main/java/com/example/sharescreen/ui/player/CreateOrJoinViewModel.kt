package com.example.sharescreen.ui.player

import androidx.lifecycle.ViewModel
import com.example.sharescreen.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CreateOrJoinViewModel @Inject constructor(
    private val roomRepository: RoomRepository
)  : ViewModel(){
    private val _uiState = MutableStateFlow(CreateOrJoinUiState())
    val uiState : StateFlow<CreateOrJoinUiState> = _uiState.asStateFlow()

    fun onRoomCodeChange(code : String) {
        _uiState.update { it.copy(roomCode = code) }
    }

    fun joinRoom() {

    }

    fun createRoom() {

    }
}