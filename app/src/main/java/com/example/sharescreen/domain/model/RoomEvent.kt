package com.example.sharescreen.domain.model

sealed class RoomEvent {

    data class Play(
        val userId : String,
        val currentTime : Long,
        val timestamp: Long
    ) : RoomEvent()

    data class Pause(
        val userId : String,
        val currentTime : Long,
        val timestamp: Long
    ) : RoomEvent()

    data class Search(
        val userId : String,
        val newTime : Long,
        val timestamp: Long
    ) : RoomEvent()

    data class Join(
        val user : User,
    ) : RoomEvent()

    data class Leave(
        val userId : String,
    ) : RoomEvent()
}