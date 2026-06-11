package com.example.sharescreen.domain.model

enum class ParticipantRole {
    HOST,
    VIEWER
}

data class Participant(
    val id: String,
    val name: String,
    val role: ParticipantRole
)
