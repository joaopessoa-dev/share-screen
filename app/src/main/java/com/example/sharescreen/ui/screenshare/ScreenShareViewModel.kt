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
    val hostParticipantId: String = "",  // ID do HOST na sessão WebSocket
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
                // hostId pode vir direto no campo ou ser encontrado na lista de participantes
                val hostId = event.hostId.ifEmpty {
                    event.participants.find { it.role == ParticipantRole.HOST }?.id ?: ""
                }
                _uiState.update {
                    it.copy(
                        participants = event.participants,
                        hostParticipantId = hostId,
                        isLoading = false
                    )
                }
            }

            is ScreenShareEvent.ParticipantJoined -> {
                _uiState.update { state ->
                    state.copy(participants = state.participants + event.participant)
                }
                // HOST: enviar offer para novo viewer se já estiver compartilhando
                if (_uiState.value.role == ParticipantRole.HOST &&
                    event.participant.role == ParticipantRole.VIEWER &&
                    _uiState.value.isScreenSharing
                ) {
                    Log.d(TAG, "New viewer joined, sending offer to ${event.participant.id}")
                    webRtcManager.createOfferFor(event.participant.id)
                }
            }

            is ScreenShareEvent.ParticipantLeft -> {
                _uiState.update { state ->
                    state.copy(participants = state.participants.filter { it.id != event.participantId })
                }
            }

            is ScreenShareEvent.Offer -> {
                // VIEWER recebe offer do HOST
                // Prioridade: fromId da mensagem → hostParticipantId do room-state → busca na lista
                val hostId = resolveHostId(event.fromId)
                Log.d(TAG, "Processing offer from host: $hostId")
                webRtcManager.processOffer(hostId, event.sdp)
            }

            is ScreenShareEvent.Answer -> {
                // HOST recebe answer do VIEWER
                // O fromId aqui é o ID do viewer que respondeu
                val viewerId = event.fromId.ifEmpty {
                    _uiState.value.participants
                        .firstOrNull { it.role == ParticipantRole.VIEWER }?.id ?: ""
                }
                Log.d(TAG, "Processing answer from viewer: $viewerId")
                webRtcManager.processAnswer(viewerId, event.sdp)
            }

            is ScreenShareEvent.IceCandidate -> {
                // Determina o peerId correto conforme o papel local
                val peerId = if (_uiState.value.role == ParticipantRole.VIEWER) {
                    resolveHostId(event.fromId)
                } else {
                    event.fromId.ifEmpty {
                        _uiState.value.participants
                            .firstOrNull { it.role == ParticipantRole.VIEWER }?.id ?: ""
                    }
                }
                if (peerId.isNotEmpty()) {
                    webRtcManager.addIceCandidate(peerId, event.candidate, event.sdpMid, event.sdpMLineIndex)
                }
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

    /** Resolve o ID do HOST: usa fromId se válido, senão cai para o hostParticipantId do room-state */
    private fun resolveHostId(fromId: String): String {
        if (fromId.isNotEmpty()) return fromId
        val stateHostId = _uiState.value.hostParticipantId
        if (stateHostId.isNotEmpty()) return stateHostId
        return _uiState.value.participants.find { it.role == ParticipantRole.HOST }?.id ?: ""
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

        // Envia offer para cada viewer já conectado
        _uiState.value.participants
            .filter { it.role == ParticipantRole.VIEWER }
            .also { Log.d(TAG, "Sending offers to ${it.size} viewers") }
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
