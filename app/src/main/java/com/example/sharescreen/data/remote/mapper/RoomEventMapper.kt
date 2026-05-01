package com.example.sharescreen.data.remote.mapper

import com.example.sharescreen.data.remote.model.RoomEventDto
import com.example.sharescreen.domain.model.RoomEvent
import com.example.sharescreen.domain.model.User

class RoomEventMapper {

    fun map(dto : RoomEventDto) : RoomEvent {

        return when(dto.type) {
            "PLAY" -> {
                RoomEvent.Play(
                    userId = dto.userId,
                    currentTime = dto.currentTime!!,
                    timestamp = dto.timestamp
                )
            }
            "PAUSE" -> {
                RoomEvent.Pause(
                    userId = dto.userId,
                    currentTime = dto.currentTime!!,
                    timestamp = dto.timestamp
                )
            }
            "SEARCH" ->{
                RoomEvent.Search(
                    userId = dto.userId,
                    newTime = dto.newTime!!,
                    timestamp = dto.timestamp
                )
            }
            "JOIN" -> {
                RoomEvent.Join(
                    user = User(
                        id = dto.userId,
                        name = dto.userName ?: "Unknow User"
                    )
                )
            }
            "LEAVE" -> {
                RoomEvent.Leave(
                    userId = dto.userId
                )
            }
            else -> throw IllegalArgumentException("Unknow event: ${dto.type}")
        }
    }

    fun toDto(event: RoomEvent): RoomEventDto {
        return when (event) {
            is RoomEvent.Play -> RoomEventDto(
                type = "PLAY",
                userId = event.userId,
                userName = null,
                currentTime = event.currentTime,
                timestamp = event.timestamp,
                newTime = null
            )
            is RoomEvent.Pause -> RoomEventDto(
                type = "PAUSE",
                userId = event.userId,
                userName = null,
                currentTime = event.currentTime,
                timestamp = event.timestamp,
                newTime = null
            )
            is RoomEvent.Search -> RoomEventDto(
                type = "SEARCH",
                userId = event.userId,
                userName = null,
                currentTime = null,
                timestamp = event.timestamp,
                newTime = event.newTime
            )
            is RoomEvent.Join -> RoomEventDto(
                type = "JOIN",
                userId = event.user.id,
                userName = event.user.name,
                currentTime = null,
                timestamp = System.currentTimeMillis(),
                newTime = null
            )
            is RoomEvent.Leave -> RoomEventDto(
                type = "LEAVE",
                userId = event.userId,
                userName = null,
                currentTime = null,
                timestamp = System.currentTimeMillis(),
                newTime = null
            )
        }
    }
}