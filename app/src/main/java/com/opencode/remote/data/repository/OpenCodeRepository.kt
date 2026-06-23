package com.opencode.remote.data.repository

import com.opencode.remote.data.api.dto.*
import com.opencode.remote.domain.ServerConfig
import com.opencode.remote.ui.sessions.SessionView
import kotlinx.coroutines.flow.Flow

interface OpenCodeRepository {
    fun testConnection(): Flow<Result<HealthResponse>>
    fun getSessions(): Flow<Result<List<SessionView>>>
    fun getSessionStatuses(): Flow<Result<Map<String, SessionStatus>>>
    fun getSession(id: String): Flow<Result<Session>>
    fun createSession(title: String?, parentId: String?): Flow<Result<Session>>
    fun updateSession(id: String, title: String): Flow<Result<Session>>
    fun deleteSession(id: String): Flow<Result<Unit>>
    fun getMessages(id: String, limit: Int): Flow<Result<List<MessageEnvelope>>>
    fun sendPrompt(id: String, text: String, model: ModelSelection?, agent: String?): Flow<Result<Unit>>
    fun sendCommand(id: String, command: String, args: String?, model: ModelSelection?, agent: String?): Flow<Result<CommandResponse>>
    fun abortSession(id: String): Flow<Result<Unit>>
    fun getTodos(id: String): Flow<Result<List<TodoItem>>>
    fun getDiffs(id: String): Flow<Result<List<DiffFile>>>
    fun getCommands(): Flow<Result<List<CommandInfo>>>
    fun getAgents(): Flow<Result<List<AgentOption>>>
    fun getProviders(): Flow<Result<ProvidersResponse>>
    fun getPath(): Flow<Result<PathInfo>>
    fun getCurrentProject(): Flow<Result<ProjectCurrent>>
    fun getVcs(): Flow<Result<VcsStatus>>
    fun getFileStatus(): Flow<Result<List<FileStatusEntry>>>
    fun listFiles(path: String): Flow<Result<List<FileEntry>>>
    fun setServerConfig(config: ServerConfig)
    fun getServerConfig(): Flow<ServerConfig>
}
