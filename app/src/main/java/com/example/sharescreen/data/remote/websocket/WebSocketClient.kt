package com.example.sharescreen.data.remote.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

interface WebSocketClient {
    fun connect(url: String)
    fun disconnect()
    fun send(message: String)

    val incomingMessages: Flow<String>
    val connectionState: StateFlow<ConnectionState>
}