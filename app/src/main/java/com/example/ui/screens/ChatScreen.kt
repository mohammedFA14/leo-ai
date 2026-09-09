package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatTopBar
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.MessageItem
import com.example.ui.components.PromptSuggestions
import com.example.ui.components.SessionsDrawerContent
import com.example.ui.components.SettingsDialog
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onAttachmentSelected(it) }
    }

    val pickDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAttachmentSelected(it) }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SessionsDrawerContent(
                    sessions = uiState.sessions,
                    currentSessionId = uiState.currentSessionId,
                    onSelectSession = { sessionId ->
                        viewModel.selectSession(sessionId)
                    },
                    onNewSession = {
                        viewModel.createNewSession()
                    },
                    onDeleteSession = { sessionId ->
                        viewModel.deleteSession(sessionId)
                    },
                    onRenameSession = { sessionId, newTitle ->
                        viewModel.renameSession(sessionId, newTitle)
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .testTag("chat_screen_scaffold"),
            topBar = {
                ChatTopBar(
                    sessionTitle = uiState.currentSession?.title ?: "محادثة ليو AI",
                    selectedModel = uiState.selectedModel,
                    cloudSyncState = syncState,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onCloudClick = {
                        viewModel.toggleCloudSyncDialog(true)
                    },
                    onSettingsClick = {
                        viewModel.toggleSettingsDialog(true)
                    },
                    onClearChatClick = {
                        viewModel.toggleClearChatConfirmDialog(true)
                    },
                    onModelClick = {
                        viewModel.toggleSettingsDialog(true)
                    }
                )
            },
            bottomBar = {
                ChatInputBar(
                    text = uiState.inputText,
                    attachment = uiState.selectedAttachment,
                    onTextChanged = { viewModel.onInputTextChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    onPickImage = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickDocument = {
                        pickDocLauncher.launch("*/*")
                    },
                    onClearAttachment = {
                        viewModel.clearAttachment()
                    },
                    isGenerating = uiState.isGenerating
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Subtle progress indicator when generating response
                AnimatedVisibility(
                    visible = uiState.isGenerating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main Chat Message Area
                if (uiState.messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PromptSuggestions(
                            onSelectPrompt = { prompt ->
                                viewModel.sendMessage(prompt)
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("messages_lazy_column")
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageItem(message = message)
                        }

                        if (uiState.isGenerating) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "⚡ ليو AI يفكر ويكتب الرد...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Cloud Sync Dialog
    if (uiState.showCloudSyncDialog) {
        CloudSyncDialog(
            syncState = syncState,
            onDismiss = { viewModel.toggleCloudSyncDialog(false) },
            onSyncNow = { viewModel.syncNow() },
            onRestoreCloud = { viewModel.restoreFromCloud() },
            onToggleAutoSync = { viewModel.toggleAutoSync(it) }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentModel = uiState.selectedModel,
            customApiKey = uiState.customApiKey,
            temperature = uiState.temperature,
            onModelSelected = { viewModel.selectModel(it) },
            onApiKeyChanged = { viewModel.updateCustomApiKey(it) },
            onTemperatureChanged = { viewModel.updateTemperature(it) },
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    // Clear Chat Confirmation Dialog
    if (uiState.showClearChatConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleClearChatConfirmDialog(false) },
            title = { Text("مسح المحادثة؟") },
            text = { Text("هل أنت متأكد من مسح جميع الرسائل في هذه المحادثة؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearCurrentSession() }
                ) {
                    Text("مسح الكل")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.toggleClearChatConfirmDialog(false) }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
