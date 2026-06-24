package com.opencode.remote.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.remote.data.api.dto.*
import com.opencode.remote.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessedMessage(
    val id: String,
    val role: String,
    val text: String,
    val thinking: String,
    val timestamp: Long
)

data class DetailUiState(
    val session: Session? = null,
    val messages: List<ProcessedMessage> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val diffs: List<DiffFile> = emptyList(),
    val project: ProjectCurrent? = null,
    val vcs: VcsStatus? = null,
    val modelOptions: List<ModelOption> = emptyList(),
    val agents: List<AgentOption> = emptyList(),
    val commands: List<CommandInfo> = emptyList(),
    val sessionStatus: SessionStatus? = null,
    val selectedModel: ModelSelection? = null,
    val selectedAgent: String? = null,
    val selectedModelOption: ModelOption? = null,
    val selectedAgentOption: AgentOption? = null,
    val composerText: String = "",
    val isBusy: Boolean = false,
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,
    val showThinking: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: OpenCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var statusPollingJob: Job? = null
    private var currentSessionId: String = ""

    fun loadSession(sessionId: String) {
        if (sessionId == currentSessionId && _uiState.value.session != null) return
        currentSessionId = sessionId
        startPolling()
        startStatusPolling()
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load session details
            repository.getSession(currentSessionId)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess { session ->
                        _uiState.value = _uiState.value.copy(session = session)
                    }
                }

            // Load messages
            loadMessagesInternal()

            // Load todos
            repository.getTodos(currentSessionId)
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { todos ->
                        _uiState.value = _uiState.value.copy(todos = todos)
                    }
                }

            // Load diffs
            repository.getDiffs(currentSessionId)
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { diffs ->
                        _uiState.value = _uiState.value.copy(diffs = diffs)
                    }
                }

            // Load project
            repository.getCurrentProject()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { project ->
                        _uiState.value = _uiState.value.copy(project = project)
                    }
                }

            // Load VCS
            repository.getVcs()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { vcs ->
                        _uiState.value = _uiState.value.copy(vcs = vcs)
                    }
                }

            // Load agents
            repository.getAgents()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { agents ->
                        val visibleAgents = agents.filter { it.hidden != true }
                        _uiState.value = _uiState.value.copy(agents = visibleAgents)
                    }
                }

            // Load model options
            repository.getModelOptions()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { options ->
                        val current = _uiState.value
                        _uiState.value = current.copy(modelOptions = options)

                        // Auto-select matching model option from session
                        val sessionModel = current.session?.model
                        if (sessionModel != null && current.selectedModelOption == null) {
                            val match = options.find { opt ->
                                opt.providerID == sessionModel.providerID &&
                                opt.modelID == sessionModel.id &&
                                opt.variant == sessionModel.variant
                            }
                            if (match != null) {
                                _uiState.value = _uiState.value.copy(selectedModelOption = match)
                            }
                        }
                    }
                }

            // Load commands
            repository.getCommands()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { commands ->
                        _uiState.value = _uiState.value.copy(commands = commands)
                    }
                }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadMessagesInternal() {
        viewModelScope.launch {
            repository.getMessages(currentSessionId, 100)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess { envelopes ->
                        _uiState.value = _uiState.value.copy(
                            messages = processMessages(envelopes)
                        )
                    }
                }
        }
    }

    private fun processMessages(envelopes: List<MessageEnvelope>): List<ProcessedMessage> {
        return envelopes.mapNotNull { envelope ->
            val textParts = mutableListOf<String>()
            val thinkingParts = mutableListOf<String>()

            // Also handle inline  thinking...  /  /thinking patterns
            var hasThinkingTag = false

            for (part in envelope.parts) {
                when (part.type) {
                    "text" -> {
                        val text = part.text ?: ""
                        // Check for inline thinking tags
                        val cleaned = extractInlineThinking(text, thinkingParts)
                        textParts.add(cleaned)
                    }
                    "thinking", "thought" -> {
                        thinkingParts.add(part.text ?: "")
                        hasThinkingTag = true
                    }
                    "omission" -> { /* skip */ }
                    else -> {
                        // Unknown type, treat as text
                        textParts.add(part.text ?: "")
                    }
                }
            }

            // Filter out empty messages
            val text = textParts.joinToString("\n").trim()
            val thinking = thinkingParts.joinToString("\n").trim()

            if (text.isEmpty() && thinking.isEmpty()) return@mapNotNull null

            ProcessedMessage(
                id = envelope.info.id,
                role = envelope.info.role,
                text = text,
                thinking = thinking,
                timestamp = envelope.info.time.created
            )
        }
    }

    /**
     * Extracts inline  thinking.../thinking patterns from text.
     * Returns the text without thinking sections, and appends thinking content to thinkingParts.
     */
    private fun extractInlineThinking(text: String, thinkingParts: MutableList<String>): String {
        val sb = StringBuilder()
        val regex = Regex("""<thinking>([\s\S]*?)</thinking>""")
        var lastEnd = 0
        for (match in regex.findAll(text)) {
            // Append text before this thinking block
            if (lastEnd < match.range.first) {
                sb.append(text.substring(lastEnd, match.range.first))
            }
            thinkingParts.add(match.groupValues[1].trim())
            lastEnd = match.range.last + 1
        }
        // Append remaining text after last thinking block
        if (lastEnd < text.length) {
            sb.append(text.substring(lastEnd))
        }
        return sb.toString()
    }

    fun refreshMessages() {
        loadMessagesInternal()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                loadMessagesInternal()
            }
        }
    }

    private fun startStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                checkSessionStatus()
            }
        }
    }

    private suspend fun checkSessionStatus() {
        repository.getSessionStatuses()
            .catch { /* ignore */ }
            .collect { result ->
                result.onSuccess { statuses ->
                    val status = statuses[currentSessionId]
                    val isBusy = status?.type == "busy" || status?.type == "retry"
                    _uiState.value = _uiState.value.copy(
                        sessionStatus = status,
                        isBusy = isBusy
                    )
                }
            }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        statusPollingJob?.cancel()
        statusPollingJob = null
    }

    fun sendMessage(text: String) {
        if (text.startsWith("/")) {
            sendSlashCommand(text)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, isBusy = true)
            repository.sendPrompt(
                id = currentSessionId,
                text = text,
                model = _uiState.value.selectedModel,
                agent = _uiState.value.selectedAgent
            )
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        isBusy = false,
                        error = e.message
                    )
                }
                .collect { result ->
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            composerText = ""
                        )
                        // Messages will be refreshed by polling
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            isBusy = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    private fun sendSlashCommand(text: String) {
        val parts = text.split(" ", limit = 2)
        val command = parts[0].removePrefix("/")
        val args = parts.getOrNull(1)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, isBusy = true)
            repository.sendCommand(
                id = currentSessionId,
                command = command,
                args = args,
                model = _uiState.value.selectedModel,
                agent = _uiState.value.selectedAgent
            )
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        isBusy = false,
                        error = e.message
                    )
                }
                .collect { result ->
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            composerText = ""
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            isBusy = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun abortSession() {
        viewModelScope.launch {
            repository.abortSession(currentSessionId)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(isBusy = false)
                        // Force a status check
                        checkSessionStatus()
                    }
                }
        }
    }

    fun selectAgent(agentId: String?) {
        val agent = if (agentId != null) {
            _uiState.value.agents.find { it.id == agentId }
        } else null
        _uiState.value = _uiState.value.copy(
            selectedAgent = agentId,
            selectedAgentOption = agent
        )
    }

    fun selectModel(modelOption: ModelOption?) {
        val selection = if (modelOption != null) {
            ModelSelection(
                providerID = modelOption.providerID,
                modelID = modelOption.modelID,
                variant = modelOption.variant
            )
        } else null
        _uiState.value = _uiState.value.copy(
            selectedModel = selection,
            selectedModelOption = modelOption
        )
    }

    fun toggleThinking() {
        _uiState.value = _uiState.value.copy(
            showThinking = !_uiState.value.showThinking
        )
    }

    fun updateComposerText(text: String) {
        _uiState.value = _uiState.value.copy(composerText = text)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
