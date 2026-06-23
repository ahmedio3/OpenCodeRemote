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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val session: Session? = null,
    val messages: List<MessageEnvelope> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val diffs: List<DiffFile> = emptyList(),
    val project: ProjectCurrent? = null,
    val vcs: VcsStatus? = null,
    val agents: List<AgentOption> = emptyList(),
    val providers: List<ProviderInfo> = emptyList(),
    val selectedModel: ModelSelection? = null,
    val selectedAgent: String? = null,
    val composerText: String = "",
    val isBusy: Boolean = false,
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: OpenCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentSessionId: String = ""

    fun loadSession(sessionId: String) {
        if (sessionId == currentSessionId && _uiState.value.session != null) return
        currentSessionId = sessionId
        startPolling()
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
                        _uiState.value = _uiState.value.copy(
                            session = session,
                            isBusy = false // will be updated by polling
                        )
                    }
                }

            // Load messages
            repository.getMessages(currentSessionId, 100)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess { messages ->
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }

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
                        _uiState.value = _uiState.value.copy(agents = agents)
                    }
                }

            // Load providers
            repository.getProviders()
                .catch { _ -> }
                .collect { result ->
                    result.onSuccess { providersResponse ->
                        _uiState.value = _uiState.value.copy(
                            providers = providersResponse.providers
                        )
                    }
                }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            repository.getMessages(currentSessionId, 100)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess { messages ->
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                refreshMessages()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun sendMessage(text: String) {
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

    fun abortSession() {
        viewModelScope.launch {
            repository.abortSession(currentSessionId)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { result ->
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(isBusy = false)
                    }
                }
        }
    }

    fun selectAgent(agent: String?) {
        _uiState.value = _uiState.value.copy(selectedAgent = agent)
    }

    fun selectModel(model: ModelSelection?) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
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
