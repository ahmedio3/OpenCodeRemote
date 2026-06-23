package com.opencode.remote.data.sse

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opencode.remote.data.local.SettingsDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventStream @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events

    private var eventSource: EventSource? = null
    private var shouldReconnect = true

    sealed class ServerEvent {
        data class Connected(val version: String = "") : ServerEvent()
        data class SessionUpdated(val sessionId: String, val data: JsonObject = JsonObject()) : ServerEvent()
        data class MessageAdded(val sessionId: String, val data: JsonObject = JsonObject()) : ServerEvent()
        data class Unknown(val type: String, val data: String = "") : ServerEvent()
        object Disconnected : ServerEvent()
    }

    fun connect() {
        val config = runBlocking {
            settingsDataStore.getConfig().first()
        }

        val url = "http://${config.host}:${config.port}/event"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        val client = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val factory = EventSources.createFactory(client)

        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                _events.tryEmit(ServerEvent.Connected())
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                val event = when (type) {
                    "server.connected" -> ServerEvent.Connected(version = data)
                    "session.updated" -> {
                        try {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val sessionId = json.get("sessionID")?.asString ?: ""
                            ServerEvent.SessionUpdated(sessionId, json)
                        } catch (_: Exception) {
                            ServerEvent.Unknown(type ?: "unknown", data)
                        }
                    }
                    "message.added" -> {
                        try {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            val sessionId = json.get("sessionID")?.asString ?: ""
                            ServerEvent.MessageAdded(sessionId, json)
                        } catch (_: Exception) {
                            ServerEvent.Unknown(type ?: "unknown", data)
                        }
                    }
                    else -> ServerEvent.Unknown(type ?: "unknown", data)
                }
                _events.tryEmit(event)
            }

            override fun onClosed(eventSource: EventSource) {
                _events.tryEmit(ServerEvent.Disconnected)
                if (shouldReconnect) {
                    reconnect()
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                _events.tryEmit(ServerEvent.Disconnected)
                if (shouldReconnect) {
                    reconnect()
                }
            }
        })
    }

    fun disconnect() {
        shouldReconnect = false
        eventSource?.cancel()
        eventSource = null
    }

    private fun reconnect() {
        eventSource?.cancel()
        eventSource = null
        connect()
    }
}
