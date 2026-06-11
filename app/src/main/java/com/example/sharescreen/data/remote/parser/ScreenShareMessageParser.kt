package com.example.sharescreen.data.remote.parser

import com.example.sharescreen.data.remote.model.ParticipantResponse
import com.example.sharescreen.data.remote.model.WsMessage
import com.example.sharescreen.data.remote.model.WsMessageType
import com.example.sharescreen.domain.model.Participant
import com.example.sharescreen.domain.model.ParticipantRole
import com.example.sharescreen.domain.model.ScreenShareEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScreenShareMessageParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(rawMessage: String): ScreenShareEvent {
        val message = json.decodeFromString<WsMessage>(rawMessage)

        return when (message.type) {
            WsMessageType.PING -> ScreenShareEvent.Ping

            WsMessageType.ROOM_STATE -> ScreenShareEvent.RoomState(
                participants = message.participants?.map { it.toParticipant() } ?: emptyList()
            )

            WsMessageType.PARTICIPANT_JOINED -> ScreenShareEvent.ParticipantJoined(
                participant = message.participant?.toParticipant()
                    ?: throw IllegalArgumentException("Missing participant in participant-joined")
            )

            WsMessageType.PARTICIPANT_LEFT -> ScreenShareEvent.ParticipantLeft(
                participantId = message.participantId
                    ?: throw IllegalArgumentException("Missing participantId in participant-left")
            )

            WsMessageType.OFFER -> ScreenShareEvent.Offer(
                fromId = message.targetId ?: "",
                sdp = message.sdp ?: throw IllegalArgumentException("Missing SDP in offer")
            )

            WsMessageType.ANSWER -> ScreenShareEvent.Answer(
                fromId = message.targetId ?: "",
                sdp = message.sdp ?: throw IllegalArgumentException("Missing SDP in answer")
            )

            WsMessageType.ICE_CANDIDATE -> ScreenShareEvent.IceCandidate(
                fromId = message.targetId ?: "",
                candidate = message.candidate
                    ?: throw IllegalArgumentException("Missing candidate in ice-candidate"),
                sdpMid = message.sdpMid,
                sdpMLineIndex = message.sdpMLineIndex
            )

            WsMessageType.ROOM_CLOSED -> ScreenShareEvent.RoomClosed(
                reason = message.reason
            )

            WsMessageType.ERROR -> ScreenShareEvent.Error(
                message = message.reason ?: "Unknown error"
            )

            else -> ScreenShareEvent.Error("Unknown message type: ${message.type}")
        }
    }

    fun createPong(): String {
        return json.encodeToString(WsMessage(type = WsMessageType.PONG))
    }

    fun createOffer(targetId: String, sdp: String): String {
        return json.encodeToString(
            WsMessage(
                type = WsMessageType.OFFER,
                targetId = targetId,
                sdp = sdp
            )
        )
    }

    fun createAnswer(targetId: String, sdp: String): String {
        return json.encodeToString(
            WsMessage(
                type = WsMessageType.ANSWER,
                targetId = targetId,
                sdp = sdp
            )
        )
    }

    fun createIceCandidate(
        targetId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ): String {
        return json.encodeToString(
            WsMessage(
                type = WsMessageType.ICE_CANDIDATE,
                targetId = targetId,
                candidate = candidate,
                sdpMid = sdpMid,
                sdpMLineIndex = sdpMLineIndex
            )
        )
    }

    fun createEndStream(): String {
        return json.encodeToString(WsMessage(type = WsMessageType.END_STREAM))
    }

    private fun ParticipantResponse.toParticipant(): Participant {
        return Participant(
            id = id,
            name = name,
            role = when (role.uppercase()) {
                "HOST" -> ParticipantRole.HOST
                else -> ParticipantRole.VIEWER
            }
        )
    }
}
