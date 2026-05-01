package com.example.sharescreen.player

interface Player {

    fun play()
    fun pause()
    fun searchTo(position : Long)

    fun getCurrentTime() : Long

    fun getDuration() : Long

    fun setMedia(url:String)

    fun release()
}