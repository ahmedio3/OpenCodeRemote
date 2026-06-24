package com.opencode.remote.data.api

import com.opencode.remote.data.api.dto.*
import retrofit2.http.*

interface OpenCodeApi {

    @GET("/global/health")
    suspend fun health(): HealthResponse

    @GET("/session")
    suspend fun listSessions(@Query("directory") directory: String? = null): List<Session>

    @GET("/session/status")
    suspend fun listStatuses(@Query("directory") directory: String? = null): Map<String, SessionStatus>

    @GET("/session/{id}")
    suspend fun getSession(
        @Path("id") id: String,
        @Query("directory") directory: String? = null
    ): Session

    @POST("/session")
    suspend fun createSession(
        @Body body: CreateSessionRequest,
        @Query("directory") directory: String? = null
    ): Session

    @PATCH("/session/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: UpdateSessionRequest,
        @Query("directory") directory: String? = null
    ): Session

    @DELETE("/session/{id}")
    suspend fun deleteSession(
        @Path("id") id: String,
        @Query("directory") directory: String? = null
    )

    @GET("/session/{id}/message")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("limit") limit: Int = 100,
        @Query("directory") directory: String? = null
    ): List<MessageEnvelope>

    @POST("/session/{id}/prompt_async")
    suspend fun sendPrompt(
        @Path("id") id: String,
        @Body body: SendPromptRequest,
        @Query("directory") directory: String? = null
    )

    @POST("/session/{id}/command")
    suspend fun sendCommand(
        @Path("id") id: String,
        @Body body: SendCommandRequest,
        @Query("directory") directory: String? = null
    ): MessageEnvelope

    @POST("/session/{id}/abort")
    suspend fun abortSession(
        @Path("id") id: String,
        @Query("directory") directory: String? = null
    )

    @GET("/session/{id}/todo")
    suspend fun getTodos(
        @Path("id") id: String,
        @Query("directory") directory: String? = null
    ): List<TodoItem>

    @GET("/session/{id}/diff")
    suspend fun getDiffs(
        @Path("id") id: String,
        @Query("directory") directory: String? = null
    ): List<DiffFile>

    @GET("/command")
    suspend fun getCommands(): List<CommandInfo>

    @GET("/agent")
    suspend fun getAgents(@Query("directory") directory: String? = null): List<AgentOption>

    @GET("/config/providers")
    suspend fun getProviders(@Query("directory") directory: String? = null): ProvidersResponse

    @GET("/path")
    suspend fun getPath(@Query("directory") directory: String? = null): PathInfo

    @GET("/project/current")
    suspend fun getCurrentProject(@Query("directory") directory: String? = null): ProjectCurrent

    @GET("/vcs")
    suspend fun getVcs(@Query("directory") directory: String? = null): VcsStatus

    @GET("/file/status")
    suspend fun getFileStatus(@Query("directory") directory: String? = null): List<FileStatusEntry>

    @GET("/file")
    suspend fun listFiles(
        @Query("path") path: String,
        @Query("directory") directory: String? = null
    ): List<FileEntry>
}
