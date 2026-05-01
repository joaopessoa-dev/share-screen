package com.example.sharescreen.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RoomEventDto(
    val type : String,
    val userId : String,
    val userName : String?,
    val currentTime : Long?,
    val timestamp: Long,
    val newTime : Long?
)