package com.example.sharescreen.data.remote.websocket

import kotlinx.coroutines.flow.Flow

interface WebSocketClient {
    fun connect()
    fun disconnect()
    fun send(message : String)

    val incomingMessages : Flow<String>
}