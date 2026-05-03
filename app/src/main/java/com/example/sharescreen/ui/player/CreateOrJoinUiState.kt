package com.example.sharescreen.ui.player

data class CreateOrJoinUiState(
    val roomCode : String = "",
    val isLoading : Boolean = false,
    val error : String? = null
)
