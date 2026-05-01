package com.example.sharescreen.domain.model

import com.example.sharescreen.domain.model.User

data class Room(
    val id : String,
    val hostId : String,
    val users : List<User>,
    val state : PlaybackState,
    val currentTime : Long,
    val lastUpdateTimestamp: Long
)

fun Room.isHost(userId : String) : Boolean {
    return userId == hostId
}

fun Room.getCurrentTime(now : Long) : Long {
    return if(state == PlaybackState.PLAYING){
        currentTime + (now - lastUpdateTimestamp)
    } else {
        currentTime
    }
}