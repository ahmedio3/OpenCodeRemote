package com.opencode.remote.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.remote.data.api.dto.*
import com.opencode.remote.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel = hiltViewModel(),
    sessionId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet states
    var showCommandPalette by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAgentSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Smart auto-scroll: track if user is at bottom
    var isAtBottom by remember { mutableStateOf(true) }
    val isScrolling = remember { mutableStateOf(false) }

    // Load session
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Smart auto-scroll: scroll to bottom only if user is at bottom
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.id, uiState.isSending) {
        if (isAtBottom && uiState.messages.isNotEmpty()) {
            // Small delay to let layout settle
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Detect scroll position
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            isScrolling.value = false
            // Check if we're at bottom
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            isAtBottom = lastVisibleItem != null &&
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
        }
    }

    // Show errors as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.session?.title ?: "Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.session?.directory != null) {
                            Text(
                                text = uiState.session!!.directory,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Thinking toggle
                    IconButton(onClick = { viewModel.toggleThinking() }) {
                        Icon(
                            if (uiState.showThinking) Icons.Default.Psychology else Icons.Default.Psychology,
                            contentDescription = "Toggle Thinking",
                            tint = if (uiState.showThinking)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Commands palette
                    IconButton(onClick = { showCommandPalette = true }) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Commands"
                        )
                    }
                    // Info
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            ComposerBar(
                onSend = { text -> viewModel.sendMessage(text) },
                onStop = { viewModel.abortSession() },
                onCommandPalette = { showCommandPalette = true },
                isBusy = uiState.isBusy,
                enabled = sessionId.isNotBlank(),
                commands = uiState.commands
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                LoadingIndicator(fullScreen = true)
            } else if (uiState.messages.isEmpty()) {
                EmptyState(
                    title = "No Messages",
                    subtitle = "Send a message to start the conversation"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    // Context strip (model/agent chips)
                    item {
                        ContextStrip(
                            modelOption = uiState.selectedModelOption,
                            agentOption = uiState.selectedAgentOption,
                            sessionStatus = uiState.sessionStatus,
                            onModelClick = { showModelSheet = true },
                            onAgentClick = { showAgentSheet = true },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Todo box
                    if (uiState.todos.isNotEmpty()) {
                        item {
                            TodoBox(
                                todos = uiState.todos,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // Messages
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            showThinking = uiState.showThinking,
                            onToggleThinking = { viewModel.toggleThinking() }
                        )
                    }

                    // Typing / busy indicator
                    if (uiState.isSending || uiState.isBusy) {
                        item {
                            BusyIndicator(
                                isSending = uiState.isSending,
                                sessionStatus = uiState.sessionStatus
                            )
                        }
                    }

                    // Bottom spacer for scroll anchor
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // Quick scroll-to-bottom FAB
            val coroutineScope = rememberCoroutineScope()
            AnimatedVisibility(
                visible = !isAtBottom && uiState.messages.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(uiState.messages.size - 1)
                        }
                        isAtBottom = true
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // ── Command Palette Bottom Sheet ──
    if (showCommandPalette) {
        CommandPalette(
            commands = uiState.commands,
            onDismiss = { showCommandPalette = false },
            onExecute = { command, _ ->
                viewModel.updateComposerText("/$command ")
            },
            selectedModelLabel = uiState.selectedModelOption?.let {
                "${it.providerName}/${it.modelName}"
            },
            selectedAgentLabel = uiState.selectedAgentOption?.name
        )
    }

    // ── Info Bottom Sheet ──
    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            SessionInfoSheet(
                session = uiState.session,
                project = uiState.project,
                vcs = uiState.vcs,
                diffs = uiState.diffs,
                sessionStatus = uiState.sessionStatus,
                modelOption = uiState.selectedModelOption,
                agentOption = uiState.selectedAgentOption
            )
        }
    }

    // ── Model Selection Bottom Sheet ──
    if (showModelSheet) {
        ModelSelectionSheet(
            modelOptions = uiState.modelOptions,
            selectedOption = uiState.selectedModelOption,
            onSelect = { option ->
                viewModel.selectModel(option)
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false }
        )
    }

    // ── Agent Selection Bottom Sheet ──
    if (showAgentSheet) {
        AgentSelectionSheet(
            agents = uiState.agents,
            selectedAgentId = uiState.selectedAgent,
            onSelect = { agentId ->
                viewModel.selectAgent(agentId)
                showAgentSheet = false
            },
            onDismiss = { showAgentSheet = false }
        )
    }
}

// ── Context Strip ──

@Composable
private fun ContextStrip(
    modelOption: ModelOption?,
    agentOption: AgentOption?,
    sessionStatus: SessionStatus?,
    onModelClick: () -> Unit,
    onAgentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (modelOption == null && agentOption == null && sessionStatus == null) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Model chip
        if (modelOption != null) {
            InputChip(
                selected = false,
                onClick = onModelClick,
                label = {
                    Text(
                        text = modelOption.modelName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Agent chip
        if (agentOption != null) {
            InputChip(
                selected = false,
                onClick = onAgentClick,
                label = {
                    Text(
                        text = agentOption.name,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Status badge
        if (sessionStatus != null && sessionStatus.type != "idle") {
            val (color, label) = when (sessionStatus.type) {
                "busy" -> MaterialTheme.colorScheme.primary to "Busy"
                "retry" -> MaterialTheme.colorScheme.error to "Retry"
                else -> MaterialTheme.colorScheme.tertiary to sessionStatus.type
            }
            SuggestionChip(
                onClick = {},
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = color
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Context limit info if available
        modelOption?.contextLimit?.let { limit ->
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = formatTokenLimit(limit),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        Icons.Default.DataArray,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ── Busy Indicator ──

@Composable
private fun BusyIndicator(
    isSending: Boolean,
    sessionStatus: SessionStatus?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isSending) "Sending..." else
                sessionStatus?.message ?: "Processing...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (sessionStatus?.attempt != null && sessionStatus.attempt > 1) {
            Text(
                text = "attempt ${sessionStatus.attempt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Session Info Sheet ──

@Composable
private fun SessionInfoSheet(
    session: Session?,
    project: ProjectCurrent?,
    vcs: VcsStatus?,
    diffs: List<DiffFile>,
    sessionStatus: SessionStatus?,
    modelOption: ModelOption?,
    agentOption: AgentOption?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Session Info",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // Model info
        if (modelOption != null) {
            InfoCard(
                title = "Model",
                content = buildString {
                    append("${modelOption.providerName} / ${modelOption.modelName}")
                    if (modelOption.variant != null) append(" (${modelOption.variant})")
                },
                subtitle = buildString {
                    if (modelOption.contextLimit != null) {
                        append("Context: ${formatTokenLimit(modelOption.contextLimit)}")
                    }
                    if (modelOption.outputLimit != null) {
                        append(" | Output: ${formatTokenLimit(modelOption.outputLimit)}")
                    }
                    append(" | Tools: ${if (modelOption.tools) "✓" else "✗"}")
                },
                icon = Icons.Default.SmartToy
            )
        } else session?.model?.let { m ->
            InfoCard(
                title = "Model",
                content = "${m.providerID}/${m.id}",
                subtitle = m.variant?.let { "Variant: $it" },
                icon = Icons.Default.SmartToy
            )
        }

        // Agent info
        if (agentOption != null) {
            InfoCard(
                title = "Agent",
                content = agentOption.name,
                subtitle = agentOption.description,
                icon = Icons.Default.Person
            )
        }

        // Project info
        project?.let { p ->
            InfoCard(
                title = "Project",
                content = p.name ?: p.directory ?: "N/A",
                icon = Icons.Default.Folder
            )
        }

        // VCS info
        vcs?.let { v ->
            InfoCard(
                title = "Git",
                content = v.branch ?: "N/A",
                subtitle = buildString {
                    append("Ahead: ${v.ahead ?: 0} | Behind: ${v.behind ?: 0}")
                    if (v.status != null) append(" | ${v.status}")
                },
                icon = Icons.Default.Commit
            )
        }

        // Session status
        sessionStatus?.let { s ->
            InfoCard(
                title = "Status",
                content = s.type,
                subtitle = s.message,
                icon = if (s.type == "busy") Icons.Default.PlayArrow else Icons.Default.CheckCircle
            )
        }

        // Diffs
        if (diffs.isNotEmpty()) {
            Text(
                text = "File Changes (${diffs.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            diffs.take(10).forEach { diff ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = diff.file,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${diff.additions}/-${diff.deletions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Session ID
        session?.let { s ->
            Text(
                text = "ID: ${s.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Model Selection Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectionSheet(
    modelOptions: List<ModelOption>,
    selectedOption: ModelOption?,
    onSelect: (ModelOption) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(searchQuery, modelOptions) {
        if (searchQuery.isBlank()) modelOptions
        else modelOptions.filter { opt ->
            opt.modelName.contains(searchQuery, ignoreCase = true) ||
            opt.providerName.contains(searchQuery, ignoreCase = true) ||
            opt.providerID.contains(searchQuery, ignoreCase = true)
        }
    }

    val grouped = remember(filteredOptions) {
        filteredOptions.groupBy { it.providerName }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

            Text(
                text = "Select Model",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search models...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                grouped.forEach { (providerName, options) ->
                    item {
                        Text(
                            text = providerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(options, key = { "${it.providerID}/${it.modelID}/${it.variant}" }) { option ->
                        val isSelected = selectedOption?.let {
                            it.providerID == option.providerID &&
                            it.modelID == option.modelID &&
                            it.variant == option.variant
                        } == true

                        Card(
                            onClick = { onSelect(option) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 0.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = option.modelName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (option.isDefault) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        "default",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                        }
                                        if (option.variant != null) {
                                            Text(
                                                text = "(${option.variant})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    // Model capabilities
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (option.contextLimit != null) {
                                            Text(
                                                text = formatTokenLimit(option.contextLimit),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (option.tools) {
                                            Text(
                                                text = "Tools",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        if (option.attachments) {
                                            Text(
                                                text = "Attachments",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Agent Selection Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSelectionSheet(
    agents: List<AgentOption>,
    selectedAgentId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

            Text(
                text = "Select Agent",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Default option (no agent)
            Card(
                onClick = { onSelect(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAgentId == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Default (No Agent)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Let the model decide",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedAgentId == null) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(agents, key = { it.id }) { agent ->
                    val isSelected = selectedAgentId == agent.id
                    Card(
                        onClick = { onSelect(agent.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = agent.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (agent.description != null) {
                                    Text(
                                        text = agent.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                Text(
                                    text = "Mode: ${agent.mode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ──

private fun formatTokenLimit(limit: Long): String {
    return when {
        limit >= 1_000_000 -> "${limit / 1_000_000}M"
        limit >= 1_000 -> "${limit / 1_000}K"
        else -> limit.toString()
    }
}
