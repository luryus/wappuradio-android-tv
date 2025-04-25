package com.lkoskela.wappuradio

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Serializable
data class NowPlayingData(val song: String)

private const val TAG = "NowPlayingClient"

class NowPlayingClient {

    @OptIn(ExperimentalSerializationApi::class)
    private val client: HttpClient = HttpClient(getDefaultHttpClientEngine()) {
        install(ContentNegotiation) {
            json(Json {
                namingStrategy = JsonNamingStrategy.SnakeCase
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    private suspend fun getNowPlaying(): NowPlayingData? {

        Log.d(TAG, "getNowPlaying")

        try {
            val res = client.get("https://wappuradio.fi/api/nowplaying")

            if (!res.status.isSuccess()) {
                Log.w(TAG, "getNowPlaying: error fetching: ${res.status}")
                return null
            }

            return res.body<NowPlayingData>()
        } catch (ex: Exception) {
            Log.e(TAG, "getNowPlaying: error fetching data", ex)
            return null
        }
    }

    val nowPlayingFlow: Flow<NowPlayingData?> = getNowPlayingFlow(20_000)

    private fun getNowPlayingFlow(intervalMs: Long): Flow<NowPlayingData?> = flow {
        Log.d(TAG, "getNowPlayingFlow")
        while (true) {
            val data = getNowPlaying()
            emit(data)
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun getDefaultHttpClientEngine() = Android.create {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }
}