package com.opencode.remote.data.repository

import com.opencode.remote.data.api.OpenCodeApi
import com.opencode.remote.data.api.dto.*
import com.opencode.remote.data.local.SettingsDataStore
import com.opencode.remote.domain.ServerConfig
import com.opencode.remote.ui.sessions.SessionView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCodeRepositoryImpl @Inject constructor(
    private val api: OpenCodeApi,
    private val settingsDataStore: SettingsDataStore
) : OpenCodeRepository {

    override fun testConnection(): Flow<Result<HealthResponse>> = flow {
        try {
            val result = api.health()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getSessions(): Flow<Result<List<SessionView>>> = flow {
        try {
            val sessions = api.listSessions()
            val statuses = try {
                api.listStatuses()
            } catch (_: Exception) {
                emptyMap()
            }

            val views = sessions.map { session ->
                val status = statuses[session.id]
                SessionView(
                    id = session.id,
                    title = session.title,
                    directory = session.directory,
                    updated = session.time.updated,
                    status = when (status?.type) {
                        "busy" -> "busy"
                        "retry" -> "retry"
                        else -> "idle"
                    },
                    files = session.summary?.files ?: 0,
                    additions = session.summary?.additions ?: 0,
                    deletions = session.summary?.deletions ?: 0,
                    modelLabel = session.model?.let {
                        "${it.providerID}/${it.id}"
                    }
                )
            }
            emit(Result.success(views))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getSessionStatuses(): Flow<Result<Map<String, SessionStatus>>> = flow {
        try {
            val result = api.listStatuses()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getSession(id: String): Flow<Result<Session>> = flow {
        try {
            val result = api.getSession(id)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun createSession(title: String?, parentId: String?): Flow<Result<Session>> = flow {
        try {
            val result = api.createSession(CreateSessionRequest(title = title, parentID = parentId))
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updateSession(id: String, title: String): Flow<Result<Session>> = flow {
        try {
            val result = api.updateSession(id, UpdateSessionRequest(title = title))
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun deleteSession(id: String): Flow<Result<Unit>> = flow {
        try {
            api.deleteSession(id)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getMessages(id: String, limit: Int): Flow<Result<List<MessageEnvelope>>> = flow {
        try {
            val result = api.getMessages(id, limit)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun sendPrompt(
        id: String,
        text: String,
        model: ModelSelection?,
        agent: String?
    ): Flow<Result<Unit>> = flow {
        try {
            api.sendPrompt(
                id,
                SendPromptRequest(
                    parts = listOf(PromptPart(type = "text", text = text)),
                    model = model,
                    agent = agent
                )
            )
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun sendCommand(
        id: String,
        command: String,
        args: String?,
        model: ModelSelection?,
        agent: String?
    ): Flow<Result<CommandResponse>> = flow {
        try {
            val result = api.sendCommand(
                id,
                SendCommandRequest(
                    command = command,
                    arguments = args,
                    model = model,
                    agent = agent
                )
            )
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun abortSession(id: String): Flow<Result<Unit>> = flow {
        try {
            api.abortSession(id)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getTodos(id: String): Flow<Result<List<TodoItem>>> = flow {
        try {
            val result = api.getTodos(id)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getDiffs(id: String): Flow<Result<List<DiffFile>>> = flow {
        try {
            val result = api.getDiffs(id)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getCommands(): Flow<Result<List<CommandInfo>>> = flow {
        try {
            val result = api.getCommands()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getAgents(): Flow<Result<List<AgentOption>>> = flow {
        try {
            val result = api.getAgents()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getProviders(): Flow<Result<ProvidersResponse>> = flow {
        try {
            val result = api.getProviders()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getPath(): Flow<Result<PathInfo>> = flow {
        try {
            val result = api.getPath()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getCurrentProject(): Flow<Result<ProjectCurrent>> = flow {
        try {
            val result = api.getCurrentProject()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getVcs(): Flow<Result<VcsStatus>> = flow {
        try {
            val result = api.getVcs()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getFileStatus(): Flow<Result<List<FileStatusEntry>>> = flow {
        try {
            val result = api.getFileStatus()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun listFiles(path: String): Flow<Result<List<FileEntry>>> = flow {
        try {
            val result = api.listFiles(path)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun setServerConfig(config: ServerConfig) {
        // This is handled by the repository/hilt rebuilding the API instance
    }

    override fun getServerConfig(): Flow<ServerConfig> {
        return settingsDataStore.getConfig()
    }
}
