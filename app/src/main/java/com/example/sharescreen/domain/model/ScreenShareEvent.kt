package com.example.sharescreen.domain.model

sealed class ScreenShareEvent {

    // Estado inicial da sala ao conectar
    data class RoomState(
        val participants: List<Participant>,
        val hostId: String = ""
    ) : ScreenShareEvent()

    // Participante entrou na sala
    data class ParticipantJoined(
        val participant: Participant
    ) : ScreenShareEvent()

    // Participante saiu da sala
    data class ParticipantLeft(
        val participantId: String
    ) : ScreenShareEvent()

    // Offer WebRTC (HOST -> VIEWER via servidor)
    data class Offer(
        val fromId: String,
        val sdp: String
    ) : ScreenShareEvent()

    // Answer WebRTC (VIEWER -> HOST via servidor)
    data class Answer(
        val fromId: String,
        val sdp: String
    ) : ScreenShareEvent()

    // ICE Candidate (bidirecional)
    data class IceCandidate(
        val fromId: String,
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int?
    ) : ScreenShareEvent()

    // Sala fechada pelo host
    data class RoomClosed(
        val reason: String?
    ) : ScreenShareEvent()

    // Erro do servidor
    data class Error(
        val message: String
    ) : ScreenShareEvent()

    // Ping do servidor (precisa responder com pong)
    data object Ping : ScreenShareEvent()
}
