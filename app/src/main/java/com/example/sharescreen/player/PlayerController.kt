package com.example.sharescreen.player

import com.example.sharescreen.domain.sync.SyncAction

class PlayerController(
    private val player : Player
) {
    fun applyAction(action : SyncAction) {
        when(action) {
            is SyncAction.Play -> player.play()
            is SyncAction.Pause -> player.pause()
            is SyncAction.Search -> player.searchTo(action.position)
            SyncAction.None -> Unit
        }
    }

    fun getCurrentTime() : Long {
        return player.getCurrentTime()
    }

    fun setMedia(url : String) {
        player.setMedia(url)
    }

    fun release() {
        player.release()
    }
}