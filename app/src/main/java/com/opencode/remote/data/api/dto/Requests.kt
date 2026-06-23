package com.opencode.remote.data.api.dto

import com.google.gson.annotations.SerializedName

data class CreateSessionRequest(
    val title: String? = null,
    @SerializedName("parentID") val parentID: String? = null
)

data class UpdateSessionRequest(
    val title: String
)

data class SendPromptRequest(
    val parts: List<PromptPart>,
    val model: ModelSelection? = null,
    val agent: String? = null,
    val variant: String? = null
)

data class PromptPart(
    val type: String,
    val text: String
)

data class ModelSelection(
    @SerializedName("providerID") val providerID: String,
    @SerializedName("modelID") val modelID: String,
    val variant: String? = null
)

data class SendCommandRequest(
    val command: String,
    val arguments: String? = null,
    val model: ModelSelection? = null,
    val agent: String? = null
)

data class CommandResponse(
    val info: MessageInfo,
    val parts: List<MessagePart>
)
