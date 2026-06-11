package com.example.sharescreen.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Auth ===

@Serializable
data class DeviceAuthRequest(
    val deviceId: String
)

@Serializable
data class DeviceAuthResponse(
    val token: String,
    val expiresIn: Long = 86400 // 24h default
)

// === Room ===

@Serializable
data class CreateRoomRequest(
    val hostName: String
)

@Serializable
data class RoomResponse(
    val roomCode: String,
    val roomId: String = "",       // server uses roomId internally
    val hostId: String = "",
    val hostName: String = "",
    val createdAt: String = "",
    val participants: List<ParticipantResponse> = emptyList()
)

@Serializable
data class ParticipantResponse(
    val id: String,
    val name: String,
    val role: String // "HOST" or "VIEWER"
)

// === WebSocket Messages ===

@Serializable
data class WsMessage(
    val type: String,
    val targetId: String? = null,
    val fromId: String? = null,   // sender's participant ID (added by relay server)
    val hostId: String? = null,   // host's participant ID (present in room-state)
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val participants: List<ParticipantResponse>? = null,
    val participant: ParticipantResponse? = null,
    val participantId: String? = null,
    val reason: String? = null
)

// WebSocket message types
object WsMessageType {
    const val PING = "ping"
    const val PONG = "pong"
    const val ROOM_STATE = "room-state"
    const val PARTICIPANT_JOINED = "participant-joined"
    const val PARTICIPANT_LEFT = "participant-left"
    const val OFFER = "offer"
    const val ANSWER = "answer"
    const val ICE_CANDIDATE = "ice-candidate"
    const val END_STREAM = "end-stream"
    const val ROOM_CLOSED = "room-closed"
    const val ERROR = "error"
}
