package com.opencode.remote.data.api.dto

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    val healthy: Boolean,
    val version: String
)

data class Session(
    val id: String,
    val title: String,
    val directory: String,
    val time: SessionTime,
    val summary: SessionSummary? = null,
    val model: SessionModel? = null,
    val project: SessionProject? = null
)

data class SessionTime(
    val created: Long,
    val updated: Long
)

data class SessionSummary(
    val additions: Int,
    val deletions: Int,
    val files: Int
)

data class SessionModel(
    val id: String,
    @SerializedName("providerID") val providerID: String,
    val variant: String? = null
)

data class SessionProject(
    val id: String,
    val name: String? = null,
    val worktree: String
)

data class SessionStatus(
    val type: String,
    val attempt: Int? = null,
    val message: String? = null,
    val next: Long? = null
)

data class MessageEnvelope(
    val info: MessageInfo,
    val parts: List<MessagePart>
)

data class MessageInfo(
    val id: String,
    val role: String,
    @SerializedName("sessionID") val sessionID: String,
    val time: MessageTime
)

data class MessageTime(
    val created: Long,
    val completed: Long? = null
)

data class MessagePart(
    val id: String,
    val type: String,
    val text: String? = null
)

data class TodoItem(
    val content: String,
    val status: String,
    val priority: String,
    val id: String
)

data class DiffFile(
    val file: String,
    val additions: Int,
    val deletions: Int
)

data class CommandInfo(
    val name: String,
    val description: String? = null,
    val source: String? = null
)

data class AgentOption(
    val id: String,
    val name: String,
    val description: String? = null,
    val mode: String,
    val hidden: Boolean? = null
)

data class ProvidersResponse(
    val providers: List<ProviderInfo>,
    val default: Map<String, String>? = null
)

data class ProviderInfo(
    val id: String,
    val name: String,
    val models: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val name: String,
    val status: String? = null,
    val contextLimit: Long? = null,
    val outputLimit: Long? = null,
    val tools: Boolean? = null,
    val isDefault: Boolean? = null
)

data class PathInfo(
    val home: String,
    val state: String,
    val config: String,
    val worktree: String,
    val directory: String
)

data class ProjectCurrent(
    val name: String? = null,
    val path: String? = null,
    val directory: String? = null,
    val root: String? = null
)

data class VcsStatus(
    val branch: String? = null,
    val status: String? = null,
    val ahead: Int? = null,
    val behind: Int? = null
)

data class FileStatusEntry(
    val path: String? = null,
    val file: String? = null,
    val status: String? = null
)

data class FileEntry(
    val name: String,
    val path: String,
    val absolute: String,
    val type: String,
    val ignored: Boolean? = null
)
