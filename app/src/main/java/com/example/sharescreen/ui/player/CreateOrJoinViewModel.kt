package com.example.sharescreen.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharescreen.data.repository.ScreenShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateOrJoinViewModel @Inject constructor(
    private val repository: ScreenShareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrJoinUiState())
    val uiState: StateFlow<CreateOrJoinUiState> = _uiState.asStateFlow()

    fun onRoomCodeChange(code: String) {
        _uiState.update { it.copy(roomCode = code.uppercase()) }
    }

    fun onUserNameChange(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun createRoom() {
        val name = _uiState.value.userName.trim().ifBlank { "Host" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val room = repository.createRoom(name)
                _uiState.update { it.copy(isLoading = false, navigateToHost = NavigationEvent(room, name)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erro ao criar sala") }
            }
        }
    }

    fun joinRoom() {
        val code = _uiState.value.roomCode.trim().uppercase()
        val name = _uiState.value.userName.trim().ifBlank { "Viewer" }

        if (code.isBlank()) {
            _uiState.update { it.copy(error = "Informe o código da sala") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val room = repository.getRoom(code)
                _uiState.update { it.copy(isLoading = false, navigateToViewer = NavigationEvent(room, name)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sala não encontrada") }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToHost = null, navigateToViewer = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
