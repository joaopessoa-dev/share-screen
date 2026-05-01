package com.example.sharescreen.data.remote.parser

import com.example.sharescreen.data.remote.model.RoomEventDto
import kotlinx.serialization.json.Json

class RoomEventParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): RoomEventDto {
        return json.decodeFromString<RoomEventDto>(raw)
    }

    fun toJson(dto: RoomEventDto): String {
        return json.encodeToString(RoomEventDto.serializer(), dto)
    }
}