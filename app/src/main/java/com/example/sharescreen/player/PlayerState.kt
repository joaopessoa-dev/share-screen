package com.example.sharescreen.player

data class PlayerState(
    val isPlaying : Boolean = false,
    val currentTime : Long = 0L,
    val duration : Long = 0L,
    val isBuffering : Boolean = false
)
