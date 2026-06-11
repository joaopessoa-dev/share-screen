package com.example.sharescreen.domain.model

data class ScreenShareRoom(
    val roomCode: String,
    val hostId: String = "",
    val hostName: String = "",
    val participants: List<Participant> = emptyList()
) {
    fun getHost(): Participant? = participants.find { it.role == ParticipantRole.HOST }

    fun getViewers(): List<Participant> = participants.filter { it.role == ParticipantRole.VIEWER }

    fun findParticipant(id: String): Participant? = participants.find { it.id == id }
}
