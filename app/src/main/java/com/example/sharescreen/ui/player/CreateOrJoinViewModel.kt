package com.example.sharescreen.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharescreen.data.repository.ScreenShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class CreateOrJoinViewModel @Inject constructor(
    private val repository: ScreenShareRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CreateOrJoinViewModel"
    }

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
                Log.e(TAG, "Erro ao criar sala", e)
                val errorMessage = parseError(e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    private fun parseError(e: Exception): String {
        return when (e) {
            is HttpException -> {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "HttpException: code=$code, body=$errorBody")
                when (code) {
                    401 -> "Sessão expirada. Tente novamente."
                    403 -> "Acesso negado"
                    404 -> "Sala não encontrada"
                    500, 502, 503 -> "Servidor indisponível. Tente novamente."
                    else -> "Erro do servidor: $code"
                }
            }
            is SocketTimeoutException -> "Tempo de conexão esgotado. Verifique sua internet."
            is UnknownHostException -> "Sem conexão com a internet"
            else -> {
                Log.e(TAG, "Erro desconhecido: ${e::class.java.simpleName} - ${e.message}")
                e.message ?: "Erro desconhecido"
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
                Log.e(TAG, "Erro ao entrar na sala", e)
                val errorMessage = parseError(e)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
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
