package com.lkoskela.wappuradio

import android.util.Log
import com.lkoskela.wappuradio.NowPlayingClient.Companion.getDefaultHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.time.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Serializable
data class Program(
    val id: String,
    val start: Instant,
    val end: Instant,
    val title: String,
    val host: String,
    val prod: String,
    val desc: String,
    val photo: String,
    val thumb: String,
    val timestamp: Instant,
    val name: String,
)

private const val TAG = "ProgramClient"

class ProgramClient(clock: Clock = Clock.System) {
    private var cachedProgramEntries: List<Program>? = null

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

    private suspend fun getFullProgram(): List<Program>? {
        if (cachedProgramEntries != null) return cachedProgramEntries

        Log.d(TAG, "getFullProgram: refreshing data")

        val res = client.get("https://wappuradio.fi/api/programs")

        if (!res.status.isSuccess()) {
            Log.w(TAG, "getFullProgram: error fetching: ${res.status}")
            return null
        }

        var data = res.body<List<Program>>()
        data = data.sortedBy { it.start }
        cachedProgramEntries = data
        return data
    }

    val programFlow by lazy {
        flow {
            Log.d(TAG, "getProgramFlow")
            while (true) {
                val data = getFullProgram()
                if (data == null) {
                    // Wait a minute, and try fetching again
                    emit(null)
                    delay(1.minutes.toJavaDuration())
                    continue
                } else {
                    // Find the current program
                    val now = clock.now()
                    val currentProg = data.find { now in it.start..it.end }
                    if (currentProg == null) {
                        Log.w(TAG, "Program not found for current time")
                        // Wait some time, and try fetching again
                        emit(null)
                        delay(5.minutes.toJavaDuration())
                        continue
                    }
                    emit(currentProg)

                    val waitUntil = currentProg.end + 30.seconds
                    val waitDur = waitUntil - clock.now()
                    if (waitDur < 30.seconds) {
                        delay(30.seconds.toJavaDuration())
                    } else {
                        delay(waitDur.toJavaDuration())
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}