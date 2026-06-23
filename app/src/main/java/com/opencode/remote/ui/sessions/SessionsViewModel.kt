package com.opencode.remote.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.remote.data.api.dto.CreateSessionRequest
import com.opencode.remote.data.api.dto.HealthResponse
import com.opencode.remote.data.api.dto.SessionStatus
import com.opencode.remote.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val isConnected: Boolean = false,
    val serverVersion: String? = null,
    val sessions: List<SessionView> = emptyList(),
    val statuses: Map<String, SessionStatus> = emptyMap(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSessions: Int = 0,
    val activeSessions: Int = 0,
    val changedFiles: Int = 0
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: OpenCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        checkConnection()
        startPolling()
    }

    fun checkConnection() {
        viewModelScope.launch {
            repository.testConnection()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isConnected = false,
                        error = e.message
                    )
                }
                .collect { result ->
                    result.onSuccess { health ->
                        _uiState.value = _uiState.value.copy(
                            isConnected = health.healthy,
                            serverVersion = health.version
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isConnected = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(3500)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getSessions()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { result ->
                    result.onSuccess { sessions ->
                        val totalSessions = sessions.size
                        val activeSessions = sessions.count { it.status == "busy" || it.status == "active" }
                        val changedFiles = sessions.sumOf { it.files }

                        _uiState.value = _uiState.value.copy(
                            sessions = sessions,
                            isLoading = false,
                            error = null,
                            totalSessions = totalSessions,
                            activeSessions = activeSessions,
                            changedFiles = changedFiles
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun createSession(title: String?, parentId: String? = null) {
        viewModelScope.launch {
            repository.createSession(title, parentId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { result ->
                    result.onSuccess {
                        refresh()
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { result ->
                    result.onSuccess {
                        refresh()
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
