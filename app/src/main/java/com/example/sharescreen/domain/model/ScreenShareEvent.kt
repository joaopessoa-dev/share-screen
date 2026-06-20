package com.example.sharescreen.domain.model

sealed class ScreenShareEvent {

    data class RoomState(
        val participants: List<Participant>,
        val hostId: String = ""
    ) : ScreenShareEvent()

    data class ParticipantJoined(
        val participant: Participant
    ) : ScreenShareEvent()


    data class ParticipantLeft(
        val participantId: String
    ) : ScreenShareEvent()

    data class Offer(
        val fromId: String,
        val sdp: String
    ) : ScreenShareEvent()


    data class Answer(
        val fromId: String,
        val sdp: String
    ) : ScreenShareEvent()


    data class IceCandidate(
        val fromId: String,
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int?
    ) : ScreenShareEvent()


    data class RoomClosed(
        val reason: String?
    ) : ScreenShareEvent()


    data class Error(
        val message: String
    ) : ScreenShareEvent()


    data object Ping : ScreenShareEvent()
}
