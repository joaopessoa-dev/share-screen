package com.example.sharescreen.data.repository

import android.util.Log
import com.example.sharescreen.data.local.TokenManager
import com.example.sharescreen.data.remote.api.ShareScreenApi
import com.example.sharescreen.data.remote.model.CreateRoomRequest
import com.example.sharescreen.data.remote.model.DeviceAuthRequest
import com.example.sharescreen.data.remote.parser.ScreenShareMessageParser
import com.example.sharescreen.data.remote.websocket.ConnectionState
import com.example.sharescreen.data.remote.websocket.WebSocketClient
import com.example.sharescreen.domain.model.Participant
import com.example.sharescreen.domain.model.ParticipantRole
import com.example.sharescreen.domain.model.ScreenShareEvent
import com.example.sharescreen.domain.model.ScreenShareRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.net.URLEncoder

class ScreenShareRepository(
    private val api: ShareScreenApi,
    private val webSocketClient: WebSocketClient,
    private val tokenManager: TokenManager,
    private val messageParser: ScreenShareMessageParser
) {
    companion object {
        private const val TAG = "ScreenShareRepository"
    }

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    val events: Flow<ScreenShareEvent> = webSocketClient.incomingMessages
        .map { raw ->
            Log.d(TAG, "Raw message: $raw")
            messageParser.parse(raw)
        }
        .onEach { event ->
            // Auto-responde ao ping com pong
            if (event is ScreenShareEvent.Ping) {
                Log.d(TAG, "Received ping, sending pong")
                webSocketClient.send(messageParser.createPong())
            }
        }

    suspend fun ensureAuthenticated(): String {
        // Verifica se já temos um token válido
        val existingToken = tokenManager.getValidToken()
        if (existingToken != null) {
            Log.d(TAG, "Using existing valid token")
            return existingToken
        }

        // Obtém ou cria o deviceId
        val deviceId = tokenManager.getOrCreateDeviceId()
        Log.d(TAG, "Authenticating with deviceId: $deviceId")

        // Autentica com o servidor
        val response = api.authenticateDevice(DeviceAuthRequest(deviceId))

        // Salva o token
        tokenManager.saveToken(response.token, response.expiresIn)
        Log.d(TAG, "Token saved, expires in ${response.expiresIn}s")

        return response.token
    }

    suspend fun createRoom(hostName: String): ScreenShareRoom {
        val token = ensureAuthenticated()
        val response = api.createRoom(
            authorization = "Bearer $token",
            request = CreateRoomRequest(hostName)
        )

        // Server only returns roomCode on creation; hostId/hostName arrive via WebSocket room-state
        return ScreenShareRoom(roomCode = response.roomCode)
    }

    suspend fun getRoom(roomCode: String): ScreenShareRoom {
        val token = ensureAuthenticated()
        val response = api.getRoom(
            authorization = "Bearer $token",
            roomCode = roomCode
        )

        // For viewer: we only need roomCode to connect via WebSocket; participants come via room-state
        return ScreenShareRoom(
            roomCode = response.roomCode,
            hostId = response.hostId.ifEmpty { response.roomId },
            hostName = response.hostName,
            participants = response.participants.map {
                Participant(
                    id = it.id,
                    name = it.name,
                    role = if (it.role == "HOST") ParticipantRole.HOST else ParticipantRole.VIEWER
                )
            }
        )
    }

    suspend fun connectToRoom(roomCode: String, name: String, role: ParticipantRole) {
        val token = ensureAuthenticated()
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val roleStr = if (role == ParticipantRole.HOST) "HOST" else "VIEWER"

        val wsUrl = "${ShareScreenApi.WS_BASE_URL}/rooms/$roomCode/ws" +
                "?token=$token&name=$encodedName&role=$roleStr"

        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        webSocketClient.connect(wsUrl)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from room")
        webSocketClient.disconnect()
    }

    // === WebRTC Signaling Methods ===

    fun sendOffer(targetId: String, sdp: String) {
        val message = messageParser.createOffer(targetId, sdp)
        Log.d(TAG, "Sending offer to $targetId")
        webSocketClient.send(message)
    }

    fun sendAnswer(targetId: String, sdp: String) {
        val message = messageParser.createAnswer(targetId, sdp)
        Log.d(TAG, "Sending answer to $targetId")
        webSocketClient.send(message)
    }

    fun sendIceCandidate(targetId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        val message = messageParser.createIceCandidate(targetId, candidate, sdpMid, sdpMLineIndex)
        Log.d(TAG, "Sending ICE candidate to $targetId")
        webSocketClient.send(message)
    }

    fun endStream() {
        val message = messageParser.createEndStream()
        Log.d(TAG, "Ending stream")
        webSocketClient.send(message)
    }
}
