package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.model.AiModel
import com.example.data.repository.ChatRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.CloudSyncUiState
import com.example.util.FileAnalysisHelper
import com.example.util.FileAnalysisResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentSessionId: String? = null,
    val currentSession: ChatSessionEntity? = null,
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val selectedModel: AiModel = AiModel.GEMINI_3_5_FLASH,
    val isGenerating: Boolean = false,
    val inputText: String = "",
    val selectedAttachment: FileAnalysisResult? = null,
    val customApiKey: String = "",
    val temperature: Float = 0.7f,
    val showCloudSyncDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showClearChatConfirmDialog: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val snackbarMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    val cloudSyncManager = CloudSyncManager(application, chatDao, viewModelScope)
    val repository = ChatRepository(application, chatDao, cloudSyncManager)

    private val prefs = application.getSharedPreferences("leo_ai_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            selectedModel = AiModel.fromId(prefs.getString("selected_model", AiModel.GEMINI_3_5_FLASH.id)!!),
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            temperature = prefs.getFloat("temperature", 0.7f)
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val cloudSyncState: StateFlow<CloudSyncUiState> = cloudSyncManager.syncState

    private var messagesJob: Job? = null

    init {
        // Observe all chat sessions
        viewModelScope.launch {
            repository.allSessions.collectLatest { sessionsList ->
                _uiState.value = _uiState.value.copy(sessions = sessionsList)

                // If no session is active or current was deleted, select latest or create one
                val currentId = _uiState.value.currentSessionId
                if (currentId == null || sessionsList.none { it.id == currentId }) {
                    if (sessionsList.isNotEmpty()) {
                        selectSession(sessionsList.first().id)
                    } else {
                        createNewSession()
                    }
                } else {
                    val updatedCurrent = sessionsList.find { it.id == currentId }
                    _uiState.value = _uiState.value.copy(currentSession = updatedCurrent)
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun selectSession(sessionId: String) {
        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            currentSession = _uiState.value.sessions.find { it.id == sessionId }
        )

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collectLatest { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun createNewSession(initialTitle: String = "محادثة جديدة مع ليو") {
        viewModelScope.launch {
            val newSession = repository.createNewSession(
                title = initialTitle,
                modelUsed = _uiState.value.selectedModel.id
            )
            selectSession(newSession.id)
        }
    }

    fun onAttachmentSelected(uri: Uri) {
        viewModelScope.launch {
            val result = FileAnalysisHelper.processUri(getApplication(), uri)
            if (result != null) {
                _uiState.value = _uiState.value.copy(selectedAttachment = result)
            } else {
                showSnackbar("تعذر قراءة الملف المرفق")
            }
        }
    }

    fun clearAttachment() {
        _uiState.value = _uiState.value.copy(selectedAttachment = null)
    }

    fun sendMessage(promptText: String? = null) {
        val textToSend = (promptText ?: _uiState.value.inputText).trim()
        val attachment = _uiState.value.selectedAttachment
        val currentSessionId = _uiState.value.currentSessionId ?: return
        if ((textToSend.isBlank() && attachment == null) || _uiState.value.isGenerating) return

        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedAttachment = null,
            isGenerating = true
        )

        viewModelScope.launch {
            repository.sendMessage(
                sessionId = currentSessionId,
                prompt = textToSend,
                model = _uiState.value.selectedModel,
                attachment = attachment,
                customApiKey = _uiState.value.customApiKey.takeIf { it.isNotBlank() },
                temperature = _uiState.value.temperature
            )
            _uiState.value = _uiState.value.copy(isGenerating = false)
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                val remaining = _uiState.value.sessions.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun clearCurrentSession() {
        val currentId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            repository.clearSessionMessages(currentId)
            _uiState.value = _uiState.value.copy(showClearChatConfirmDialog = false)
            showSnackbar("تم مسح محادثة ليو الحالية")
        }
    }

    fun selectModel(model: AiModel) {
        prefs.edit().putString("selected_model", model.id).apply()
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun updateCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        _uiState.value = _uiState.value.copy(customApiKey = key)
    }

    fun updateTemperature(temperature: Float) {
        prefs.edit().putFloat("temperature", temperature).apply()
        _uiState.value = _uiState.value.copy(temperature = temperature)
    }

    fun toggleCloudSyncDialog(show: Boolean) {
        if (show) {
            cloudSyncManager.refreshSyncState()
        }
        _uiState.value = _uiState.value.copy(showCloudSyncDialog = show)
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    fun toggleClearChatConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearChatConfirmDialog = show)
    }

    fun syncNow() {
        cloudSyncManager.syncNow { success, message ->
            showSnackbar(message)
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            val result = cloudSyncManager.restoreFromCloudBackup()
            result.onSuccess { count ->
                showSnackbar("تمت استعادة $count محادثات من السحابة بنجاح")
            }.onFailure { err ->
                showSnackbar("فشل الاستعادة: ${err.message}")
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        cloudSyncManager.setAutoSyncEnabled(enabled)
    }

    fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
