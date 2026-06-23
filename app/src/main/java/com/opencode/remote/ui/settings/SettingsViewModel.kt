package com.opencode.remote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.remote.data.local.SettingsDataStore
import com.opencode.remote.data.repository.OpenCodeRepository
import com.opencode.remote.domain.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val host: String = "192.168.1.100",
    val port: String = "4096",
    val username: String = "opencode",
    val password: String = "",
    val theme: String = "system",
    val language: String = "en",
    val isTestingConnection: Boolean = false,
    val connectionResult: ConnectionTestResult? = null,
    val isSaving: Boolean = false,
    val appVersion: String = "1.0"
)

sealed class ConnectionTestResult {
    object Success : ConnectionTestResult()
    data class Failure(val message: String) : ConnectionTestResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: OpenCodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val config = settingsDataStore.getConfig().first()
            val theme = settingsDataStore.getTheme().first()
            val language = settingsDataStore.getLanguage().first()

            _uiState.value = _uiState.value.copy(
                host = config.host,
                port = config.port.toString(),
                username = config.username,
                password = config.password,
                theme = theme,
                language = language
            )
        }
    }

    fun updateHost(host: String) {
        _uiState.value = _uiState.value.copy(host = host)
    }

    fun updatePort(port: String) {
        _uiState.value = _uiState.value.copy(port = port)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
        viewModelScope.launch {
            settingsDataStore.saveTheme(theme)
        }
    }

    fun updateLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
        viewModelScope.launch {
            settingsDataStore.saveLanguage(language)
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val port = _uiState.value.port.toIntOrNull() ?: 4096
            val config = ServerConfig(
                host = _uiState.value.host,
                port = port,
                username = _uiState.value.username,
                password = _uiState.value.password
            )
            repository.setServerConfig(config)
            settingsDataStore.saveConfig(config)
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingConnection = true,
                connectionResult = null
            )

            // Save config first so the API instance uses the new settings
            saveConfig()

            repository.testConnection()
                .collect { result ->
                    result.onSuccess { health ->
                        _uiState.value = _uiState.value.copy(
                            isTestingConnection = false,
                            connectionResult = if (health.healthy) {
                                ConnectionTestResult.Success
                            } else {
                                ConnectionTestResult.Failure("Server returned unhealthy")
                            }
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isTestingConnection = false,
                            connectionResult = ConnectionTestResult.Failure(
                                e.message ?: "Connection failed"
                            )
                        )
                    }
                }
        }
    }

    fun clearConnectionResult() {
        _uiState.value = _uiState.value.copy(connectionResult = null)
    }
}
