package com.example.sharescreen.data.repository

import com.example.sharescreen.data.remote.mapper.RoomEventMapper
import com.example.sharescreen.data.remote.parser.RoomEventParser
import com.example.sharescreen.data.remote.websocket.WebSocketClient
import com.example.sharescreen.domain.model.RoomEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRepository(
    private val webSocketClient: WebSocketClient,
    private val parser: RoomEventParser,
    private val mapper: RoomEventMapper
) {

    fun connect() {
        webSocketClient.connect()
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    val events: Flow<RoomEvent> =
        webSocketClient.incomingMessages
            .map { json ->
                val dto = parser.parse(json)
                mapper.map(dto)
            }

    fun sendEvent(event: RoomEvent) {
        val dto = mapper.toDto(event)
        val json = parser.toJson(dto)
        webSocketClient.send(json)
    }

}
