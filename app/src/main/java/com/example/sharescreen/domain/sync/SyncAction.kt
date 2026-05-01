package com.example.sharescreen.domain.sync

sealed class SyncAction {
    object Play : SyncAction()
    object Pause : SyncAction()
    data class Search (val position : Long) : SyncAction()
    object None : SyncAction()
}