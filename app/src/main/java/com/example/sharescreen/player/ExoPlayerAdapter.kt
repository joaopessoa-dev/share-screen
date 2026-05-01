package com.example.sharescreen.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ExoPlayerAdapter(
    private val exoPLayer : ExoPlayer
) : Player {
    override fun play() {
        exoPLayer.playWhenReady = true
    }

    override fun pause() {
        exoPLayer.playWhenReady = false
    }

    override fun searchTo(position: Long) {
        exoPLayer.seekTo(position)
    }

    override fun getCurrentTime(): Long {
        return exoPLayer.currentPosition
    }

    override fun getDuration(): Long {
        return exoPLayer.duration
    }

    override fun setMedia(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        exoPLayer.setMediaItem(mediaItem)
        exoPLayer.prepare()
    }

    override fun release() {
        exoPLayer.release()
    }

}