package com.example.sharescreen.di

import android.content.Context
import com.example.sharescreen.data.local.TokenManager
import com.example.sharescreen.data.remote.api.ShareScreenApi
import com.example.sharescreen.data.remote.mapper.RoomEventMapper
import com.example.sharescreen.data.remote.parser.RoomEventParser
import com.example.sharescreen.data.remote.parser.ScreenShareMessageParser
import com.example.sharescreen.data.remote.websocket.OkHttpWebSocketClient
import com.example.sharescreen.data.remote.websocket.WebSocketClient
import com.example.sharescreen.data.repository.RoomRepository
import com.example.sharescreen.data.repository.ScreenShareRepository
import com.example.sharescreen.domain.sync.SyncManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS) // Server expects ping every 15s
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ShareScreenApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideShareScreenApi(retrofit: Retrofit): ShareScreenApi {
        return retrofit.create(ShareScreenApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient): WebSocketClient {
        return OkHttpWebSocketClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideScreenShareMessageParser(): ScreenShareMessageParser {
        return ScreenShareMessageParser()
    }

    @Provides
    @Singleton
    fun provideScreenShareRepository(
        api: ShareScreenApi,
        webSocketClient: WebSocketClient,
        tokenManager: TokenManager,
        messageParser: ScreenShareMessageParser
    ): ScreenShareRepository {
        return ScreenShareRepository(api, webSocketClient, tokenManager, messageParser)
    }

    // === Legacy providers for existing code ===

    @Provides
    @Singleton
    fun provideRoomEventParser(): RoomEventParser = RoomEventParser()

    @Provides
    @Singleton
    fun provideRoomEventMapper(): RoomEventMapper = RoomEventMapper()

    @Provides
    @Singleton
    fun provideRoomRepository(
        webSocketClient: WebSocketClient,
        parser: RoomEventParser,
        mapper: RoomEventMapper
    ): RoomRepository = RoomRepository(webSocketClient, parser, mapper)

    @Provides
    @Singleton
    fun provideSyncManager(): SyncManager = SyncManager()
}
