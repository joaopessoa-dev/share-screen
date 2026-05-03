package com.example.sharescreen.ui.room

import com.example.sharescreen.domain.model.Room

data class RoomUiState(
    val room : Room? = null,
    val currentUserId : String = "",
    val isConnected : Boolean = false,
    val error : String? = null
)
