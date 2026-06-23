package com.opencode.remote.data.api

import com.opencode.remote.data.api.dto.*
import retrofit2.http.*

interface OpenCodeApi {

    @GET("/global/health")
    suspend fun health(): HealthResponse

    @GET("/session")
    suspend fun listSessions(): List<Session>

    @GET("/session/status")
    suspend fun listStatuses(): Map<String, SessionStatus>

    @GET("/session/{id}")
    suspend fun getSession(@Path("id") id: String): Session

    @POST("/session")
    suspend fun createSession(@Body body: CreateSessionRequest): Session

    @PATCH("/session/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: UpdateSessionRequest
    ): Session

    @DELETE("/session/{id}")
    suspend fun deleteSession(@Path("id") id: String)

    @GET("/session/{id}/message")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("limit") limit: Int = 100
    ): List<MessageEnvelope>

    @POST("/session/{id}/prompt_async")
    suspend fun sendPrompt(
        @Path("id") id: String,
        @Body body: SendPromptRequest
    )

    @POST("/session/{id}/command")
    suspend fun sendCommand(
        @Path("id") id: String,
        @Body body: SendCommandRequest
    ): CommandResponse

    @POST("/session/{id}/abort")
    suspend fun abortSession(@Path("id") id: String)

    @GET("/session/{id}/todo")
    suspend fun getTodos(@Path("id") id: String): List<TodoItem>

    @GET("/session/{id}/diff")
    suspend fun getDiffs(@Path("id") id: String): List<DiffFile>

    @GET("/command")
    suspend fun getCommands(): List<CommandInfo>

    @GET("/agent")
    suspend fun getAgents(): List<AgentOption>

    @GET("/config/providers")
    suspend fun getProviders(): ProvidersResponse

    @GET("/path")
    suspend fun getPath(): PathInfo

    @GET("/project/current")
    suspend fun getCurrentProject(): ProjectCurrent

    @GET("/vcs")
    suspend fun getVcs(): VcsStatus

    @GET("/file/status")
    suspend fun getFileStatus(): List<FileStatusEntry>

    @GET("/file")
    suspend fun listFiles(@Query("path") path: String): List<FileEntry>
}
