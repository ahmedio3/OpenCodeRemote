package com.opencode.remote.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opencode.remote.data.api.dto.CommandInfo
import com.opencode.remote.data.repository.OpenCodeRepository
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repository: OpenCodeRepository
) : ViewModel() {

    private val _commands = MutableStateFlow<List<CommandInfo>>(emptyList())
    val commands: StateFlow<List<CommandInfo>> = _commands.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCommands()
    }

    fun loadCommands() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getCommands()
                .collect { result ->
                    result.onSuccess { cmds ->
                        _commands.value = cmds
                    }
                    _isLoading.value = false
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    viewModel: HelpViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val commands by viewModel.commands.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val tabs = listOf("Overview", "Server", "Network", "Troubleshooting", "Commands")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab()
                1 -> ServerTab()
                2 -> NetworkTab()
                3 -> TroubleshootingTab()
                4 -> CommandsTab(commands, isLoading)
            }
        }
    }
}

@Composable
private fun OverviewTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HelpCard(
                title = "OpenCode Remote",
                content = "OpenCode Remote is a companion app for OpenCode, an AI coding agent that runs in your terminal. This app lets you monitor sessions, send prompts, view responses, manage models and agents, and control the server from your Android device."
            )
        }
        item {
            HelpCard(
                title = "Getting Started",
                content = "1. Make sure your OpenCode server is running on your machine\n2. Go to Settings and enter the server's IP address and port\n3. Enter your credentials (username/password if enabled)\n4. Tap 'Test' to verify the connection\n5. Start managing sessions from the Sessions tab"
            )
        }
        item {
            HelpCard(
                title = "Features",
                content = "• View all active coding sessions\n• Send prompts and view AI responses\n• Abort running sessions\n• View session todos and file changes\n• Select different models and agents\n• Monitor server health and status\n• Real-time updates via SSE"
            )
        }
    }
}

@Composable
private fun ServerTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HelpCard(
                title = "Server Requirements",
                content = "• OpenCode server must be running on your machine\n• Server must be accessible from your phone (same network or VPN)\n• Port 4096 (default) must be open\n• Authentication enabled if configured"
            )
        }
        item {
            HelpCard(
                title = "Starting the Server",
                content = "In Termux or your terminal:\n\nopencode server\n\nThe server will start on port 4096 by default. You can change the port with:\n\nopencode server --port 8080"
            )
        }
        item {
            HelpCard(
                title = "Health Check",
                content = "The app periodically checks the server health via the /global/health endpoint. A green indicator means the server is healthy and reachable."
            )
        }
    }
}

@Composable
private fun NetworkTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HelpCard(
                title = "Network Configuration",
                content = "The app communicates with the OpenCode server over HTTP on your local network. Ensure your phone and the server machine are on the same network."
            )
        }
        item {
            HelpCard(
                title = "Firewall",
                content = "Make sure your firewall allows incoming connections on the OpenCode server port. On Linux: sudo ufw allow 4096"
            )
        }
        item {
            HelpCard(
                title = "Troubleshooting Connection",
                content = "1. Verify the server IP address is correct\n2. Check that the server is running\n3. Ensure both devices are on the same network\n4. Check firewall settings\n5. Try pinging the server from Termux: ping <server-ip>"
            )
        }
    }
}

@Composable
private fun TroubleshootingTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HelpCard(
                title = "Connection Issues",
                content = "• Verify the server is running (opencode server)\n• Check the IP address and port in Settings\n• Ensure both devices are on the same network\n• Try using the host machine's local IP (not localhost)\n• Disable any VPN or firewall temporarily"
            )
        }
        item {
            HelpCard(
                title = "Authentication Issues",
                content = "• Verify username and password in Settings\n• Check if authentication is enabled on the server\n• Credentials are sent as Basic Auth\n• No encryption - use only on trusted networks"
            )
        }
        item {
            HelpCard(
                title = "Blank Screen / No Sessions",
                content = "• Check server connection status indicator\n• Refresh the sessions list manually\n• Create a new session on the server first\n• Check server logs for errors"
            )
        }
        item {
            HelpCard(
                title = "App Crashes / Errors",
                content = "• Try restarting the app\n• Check the server version compatibility\n• Report issues on GitHub with server logs"
            )
        }
    }
}

@Composable
private fun CommandsTab(
    commands: List<CommandInfo>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (commands.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No commands available.\nConnect to a server to see available commands.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commands) { cmd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = cmd.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (cmd.description != null) {
                            Text(
                                text = cmd.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 26.dp, top = 4.dp)
                            )
                        }
                        if (cmd.source != null) {
                            Text(
                                text = "Source: ${cmd.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
