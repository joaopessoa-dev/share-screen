package com.example.sharescreen.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharescreen.data.repository.RoomRepository
import com.example.sharescreen.domain.model.RoomEvent
import com.example.sharescreen.domain.sync.SyncManager
import com.example.sharescreen.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val repository: RoomRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState : StateFlow<RoomUiState> = _uiState.asStateFlow()

    private var playerController : PlayerController? = null

    fun setPlayerController(controller : PlayerController) {
        this.playerController = controller
    }

    fun connect(url: String) {
        repository.connect(url)
        viewModelScope.launch {
            repository.events.collect { event ->
                val playerTime = playerController?.getCurrentTime() ?: 0L
                val (room, action) = syncManager.onEvent(event,System.currentTimeMillis(),playerTime)
                playerController?.applyAction(action)
                _uiState.update { it.copy(room = room) }
            }
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun onPlay() {
        val event = RoomEvent.Play(
            userId = _uiState.value.currentUserId,
            currentTime = playerController?.getCurrentTime() ?: 0L,
            timestamp = System.currentTimeMillis()
        )
        repository.sendEvent(event)
    }

    fun pause() {
        val event = RoomEvent.Pause(
            userId = _uiState.value.currentUserId,
            currentTime = playerController?.getCurrentTime() ?: 0L,
            timestamp = System.currentTimeMillis()
        )
        repository.sendEvent(event)
    }

    fun onSearch (position : Long) {
        val event = RoomEvent.Search(
            userId = _uiState.value.currentUserId,
            newTime = position,
            timestamp = System.currentTimeMillis()
        )
        repository.sendEvent(event)
    }

    override fun onCleared() {
        disconnect()
        playerController?.release()
    }
}