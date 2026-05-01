package com.example.sharescreen.domain.sync


import com.example.sharescreen.domain.model.PlaybackState
import com.example.sharescreen.domain.model.Room
import com.example.sharescreen.domain.model.RoomEvent
import com.example.sharescreen.domain.model.getCurrentTime


class SyncManager {

    private var currentRoom : Room? = null
    private var lastPlaybackState: PlaybackState? = null

    fun onEvent(roomEvent : RoomEvent, now : Long,playerTime : Long): Pair<Room, SyncAction>  {
        val updateRoom = reduceEvent(currentRoom,roomEvent)
        currentRoom = updateRoom

        val action = calculateAction(updateRoom,now, playerTime)

        return updateRoom to action

    }
    fun heartbeat(
        now: Long,
        playerTime: Long
    ): SyncAction? {
        val room = currentRoom ?: return null
        return calculateAction(room, now, playerTime)
    }

    fun calculateAction(
        room: Room,
        now: Long,
        playerTime : Long
    ) : SyncAction {
        val expectedTime = room.getCurrentTime(now)
        val diff = expectedTime - playerTime

        val threshold = 1000L

        val action =  when(room.state) {
            PlaybackState.PLAYING -> {

                if(kotlin.math.abs(diff) > threshold) {
                    return SyncAction.Search(expectedTime)
                } else if(lastPlaybackState != PlaybackState.PLAYING) {
                    SyncAction.Play
                } else {
                    SyncAction.None
                }
                SyncAction.Play
            }
            PlaybackState.PAUSED -> {
               if(lastPlaybackState != PlaybackState.PAUSED) {
                   SyncAction.Pause
               } else {
                   SyncAction.None
               }
            }
            PlaybackState.BUFFERING ->  {
                SyncAction.None
            }
        }

        lastPlaybackState = room.state
        return action

    }
    fun reduceEvent(room : Room?, roomEvent : RoomEvent) : Room {
        val current = room ?: error("Room not initialized")

        return when (roomEvent){
            is RoomEvent.Play ->  {
                current.copy(
                    state = PlaybackState.PLAYING,
                    currentTime = roomEvent.currentTime,
                    lastUpdateTimestamp = roomEvent.timestamp
                )
            }

            is RoomEvent.Join -> {
                current.copy(
                    users = current.users + roomEvent.user
                )
            }

            is RoomEvent.Search -> {
                current.copy(
                    currentTime = roomEvent.newTime,
                    lastUpdateTimestamp = roomEvent.timestamp
                )
            }

            is RoomEvent.Leave -> {
                current.copy(
                    users = current.users.filterNot { it.id == roomEvent.userId }
                )
            }

            is RoomEvent.Pause -> {
                current.copy(
                    state = PlaybackState.PAUSED,
                    currentTime = roomEvent.currentTime,
                    lastUpdateTimestamp = roomEvent.timestamp
                )
            }
        }
    }
}