package com.example.sharescreen.di

import com.example.sharescreen.data.remote.mapper.RoomEventMapper
import com.example.sharescreen.data.remote.parser.RoomEventParser
import com.example.sharescreen.data.remote.websocket.OkHttpWebSocketClient
import com.example.sharescreen.data.remote.websocket.WebSocketClient
import com.example.sharescreen.data.repository.RoomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWebSocketClient() : WebSocketClient {
        return OkHttpWebSocketClient("wss://my-server.com/ws") //todo create server
    }

    @Provides
    @Singleton
    fun provideRoomEventParser() : RoomEventParser = RoomEventParser()

    @Provides
    @Singleton
    fun provideRoomEventMapper() : RoomEventMapper = RoomEventMapper()

    @Provides
    @Singleton
    fun provideRoomRepository(
        webSocketClient: WebSocketClient,
        parser: RoomEventParser,
        mapper: RoomEventMapper
    ) : RoomRepository = RoomRepository(webSocketClient,parser,mapper)

}