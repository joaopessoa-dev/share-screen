package com.example.sharescreen.data.remote.api

import com.example.sharescreen.data.remote.model.CreateRoomRequest
import com.example.sharescreen.data.remote.model.DeviceAuthRequest
import com.example.sharescreen.data.remote.model.DeviceAuthResponse
import com.example.sharescreen.data.remote.model.RoomResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ShareScreenApi {

    companion object {
        const val BASE_URL = "https://server-share-screen.fly.dev/"
        const val WS_BASE_URL = "wss://server-share-screen.fly.dev"
    }

    @POST("auth/device")
    suspend fun authenticateDevice(
        @Body request: DeviceAuthRequest
    ): DeviceAuthResponse

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") authorization: String,
        @Body request: CreateRoomRequest
    ): RoomResponse

    @GET("rooms/{roomCode}")
    suspend fun getRoom(
        @Header("Authorization") authorization: String,
        @Path("roomCode") roomCode: String
    ): RoomResponse
}
