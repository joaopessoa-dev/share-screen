package com.example.sharescreen.ui.screenshare

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharescreen.data.remote.websocket.ConnectionState
import com.example.sharescreen.data.repository.ScreenShareRepository
import com.example.sharescreen.domain.model.Participant
import com.example.sharescreen.domain.model.ParticipantRole
import com.example.sharescreen.domain.model.ScreenShareEvent
import com.example.sharescreen.domain.model.ScreenShareRoom
import com.example.sharescreen.webrtc.WebRtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import javax.inject.Inject

data class ScreenShareUiState(
    val isLoading: Boolean = false,
    val room: ScreenShareRoom? = null,
    val role: ParticipantRole? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val participants: List<Participant> = emptyList(),
    val isScreenSharing: Boolean = false,
    val error: String? = null,
    val isRoomClosed: Boolean = false
)

@HiltViewModel
class ScreenShareViewModel @Inject constructor(
    application: Application,
    private val repository: ScreenShareRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ScreenShareViewModel"
    }

    private val webRtcManager = WebRtcManager(application).also { it.initialize() }

    val eglBaseContext: EglBase.Context = webRtcManager.eglBase.eglBaseContext
    val remoteVideoTrack: StateFlow<VideoTrack?> = webRtcManager.remoteVideoTrack

    private val _uiState = MutableStateFlow(ScreenShareUiState())
    val uiState: StateFlow<ScreenShareUiState> = _uiState.asStateFlow()

    init {
        wireWebRtcCallbacks()
        observeConnectionState()
        observeEvents()
    }

    private fun wireWebRtcCallbacks() {
        webRtcManager.onOffer = { targetId, sdp ->
            Log.d(TAG, "Sending offer to $targetId")
            repository.sendOffer(targetId, sdp)
        }
        webRtcManager.onAnswer = { targetId, sdp ->
            Log.d(TAG, "Sending answer to $targetId")
            repository.sendAnswer(targetId, sdp)
        }
        webRtcManager.onIceCandidate = { targetId, candidate, sdpMid, sdpMLineIndex ->
            repository.sendIceCandidate(targetId, candidate, sdpMid, sdpMLineIndex)
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.events.collect { event ->
                Log.d(TAG, "Event: $event")
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: ScreenShareEvent) {
        when (event) {
            is ScreenShareEvent.RoomState -> {
                _uiState.update { it.copy(participants = event.participants, isLoading = false) }
            }

            is ScreenShareEvent.ParticipantJoined -> {
                _uiState.update { state ->
                    state.copy(participants = state.participants + event.participant)
                }
                // HOST: send offer to new viewer if screen share is already active
                if (_uiState.value.role == ParticipantRole.HOST &&
                    event.participant.role == ParticipantRole.VIEWER &&
                    _uiState.value.isScreenSharing
                ) {
                    webRtcManager.createOfferFor(event.participant.id)
                }
            }

            is ScreenShareEvent.ParticipantLeft -> {
                _uiState.update { state ->
                    state.copy(participants = state.participants.filter { it.id != event.participantId })
                }
            }

            is ScreenShareEvent.Offer -> {
                // VIEWER: received offer from host
                webRtcManager.processOffer(event.fromId, event.sdp)
            }

            is ScreenShareEvent.Answer -> {
                // HOST: received answer from viewer
                webRtcManager.processAnswer(event.fromId, event.sdp)
            }

            is ScreenShareEvent.IceCandidate -> {
                webRtcManager.addIceCandidate(event.fromId, event.candidate, event.sdpMid, event.sdpMLineIndex)
            }

            is ScreenShareEvent.RoomClosed -> {
                _uiState.update { it.copy(isRoomClosed = true, error = event.reason ?: "Sala encerrada") }
            }

            is ScreenShareEvent.Error -> {
                _uiState.update { it.copy(error = event.message) }
            }

            is ScreenShareEvent.Ping -> { /* handled by repository */ }
        }
    }

    // ── HOST ─────────────────────────────────────────────────────────────────

    fun connectAsHost(room: ScreenShareRoom, hostName: String) {
        _uiState.update { it.copy(room = room, role = ParticipantRole.HOST, isLoading = true) }
        viewModelScope.launch {
            try {
                repository.connectToRoom(room.roomCode, hostName, ParticipantRole.HOST)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect as HOST", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startScreenShare(mediaProjectionIntent: Intent) {
        webRtcManager.startScreenCapture(mediaProjectionIntent)
        _uiState.update { it.copy(isScreenSharing = true) }

        // Send offer to every viewer already in the room
        _uiState.value.participants
            .filter { it.role == ParticipantRole.VIEWER }
            .forEach { webRtcManager.createOfferFor(it.id) }
    }

    fun stopScreenShare() {
        webRtcManager.stopScreenCapture()
        repository.endStream()
        _uiState.update { it.copy(isScreenSharing = false) }
    }

    // ── VIEWER ────────────────────────────────────────────────────────────────

    fun connectAsViewer(roomCode: String, viewerName: String) {
        _uiState.update { it.copy(role = ParticipantRole.VIEWER, isLoading = true) }
        viewModelScope.launch {
            try {
                repository.connectToRoom(roomCode, viewerName, ParticipantRole.VIEWER)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect as VIEWER", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Common ────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getViewers(): List<Participant> =
        _uiState.value.participants.filter { it.role == ParticipantRole.VIEWER }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.release()
        repository.disconnect()
    }
}
