package com.example.sharescreen.data.remote.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class OkHttpWebSocketClient(
    private val url : String
) : WebSocketClient {

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket : WebSocket? = null
    private val _incomingMessages = MutableSharedFlow<String>()
    override val incomingMessages : Flow<String> = _incomingMessages.asSharedFlow()
    override fun connect() {
        val req = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(req,object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
               // connection opened
            }

            override fun onMessage(webSocket: WebSocket, text : String) {
                CoroutineScope(Dispatchers.IO).launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000,null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Handle failure
            }
        })
    }

    override fun disconnect() {
        webSocket?.close(1000,null)
        webSocket = null
    }

    override fun send(message: String) {
        webSocket?.send(message)
    }


}