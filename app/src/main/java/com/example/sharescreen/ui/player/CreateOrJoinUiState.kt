package com.example.sharescreen.ui.player

import com.example.sharescreen.domain.model.ScreenShareRoom

data class NavigationEvent(val room: ScreenShareRoom, val userName: String)

data class CreateOrJoinUiState(
    val roomCode: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToHost: NavigationEvent? = null,
    val navigateToViewer: NavigationEvent? = null
)
