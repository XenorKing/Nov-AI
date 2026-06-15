package com.novaproject.novai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novaproject.novai.data.model.AISettings
import com.novaproject.novai.data.model.Conversation
import com.novaproject.novai.data.model.Message
import com.novaproject.novai.data.model.MessageSearchResult
import com.novaproject.novai.data.repository.AuthRepository
import com.novaproject.novai.data.repository.ChatRepository
import com.novaproject.novai.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
    val currentModel: String = "",
    val aiName: String = "NovAI",
    val aiAvatarEmoji: String = "🤖",
    val accentColor: String = "cyan",
    val hasMoreMessages: Boolean = false,
    val isLoadingMore: Boolean = false,
    val streamingContent: String = "",
    val customModels: List<String> = emptyList()
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val messageResults: List<MessageSearchResult> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val authRepo: AuthRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _convState = MutableStateFlow(ConversationsUiState())
    val convState: StateFlow<ConversationsUiState> = _convState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, networkMonitor.hasActiveNetwork())

    private var currentSettings = AISettings()
    private var currentConvId: String? = null

    private var convsJob: Job? = null
    private var messagesJob: Job? = null
    private var settingsJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            authRepo.authState
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) { loadConversations(); subscribeSettings() }
                    else {
                        convsJob?.cancel(); settingsJob?.cancel()
                        _convState.value = ConversationsUiState()
                        currentSettings = AISettings()
                        // Vuln-3: clear HTTP response cache on logout so cached AI
                        // replies from one account are never served to another.
                        repo.clearCache()
                    }
                }
        }
    }

    private fun loadConversations() {
        convsJob?.cancel()
        convsJob = viewModelScope.launch {
            try {
                repo.conversationsFlow().collect { convs ->
                    // Merge in-memory pin states to guard against the race where
                    // the Firestore listener fires before a pinConversation write
                    // has been confirmed, which would silently drop a pin.
                    val pendingPins = _convState.value.conversations
                        .filter { it.isPinned }.map { it.id }.toSet()
                    val merged = convs.map { c ->
                        if (!c.isPinned && c.id in pendingPins) c.copy(isPinned = true) else c
                    }
                    _convState.value = _convState.value.copy(conversations = merged, error = null)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Ошибка загрузки"
                _convState.value = _convState.value.copy(
                    error = if (msg.contains("PERMISSION_DENIED")) "Нет доступа к данным. Проверьте правила Firestore." else msg
                )
            }
        }
    }

    private fun subscribeSettings() {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            repo.settingsFlow().catch { }.collect { settings ->
                currentSettings = settings
                _chatState.value = _chatState.value.copy(
                    currentModel = settings.customModel,
                    aiName = settings.aiName.ifBlank { "NovAI" },
                    aiAvatarEmoji = settings.aiAvatarEmoji.ifBlank { "🤖" },
                    accentColor = settings.accentColor.ifBlank { "cyan" },
                    customModels = settings.customModels
                )
            }
        }
    }

    // ── Conversation / chat lifecycle ──────────────────────────────────────────

    fun openConversation(convId: String) {
        if (currentConvId == convId) return
        currentConvId = convId
        _chatState.value = ChatUiState(
            currentModel = _chatState.value.currentModel,
            aiName = _chatState.value.aiName,
            aiAvatarEmoji = _chatState.value.aiAvatarEmoji,
            accentColor = _chatState.value.accentColor,
            customModels = _chatState.value.customModels
        )
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            try {
                repo.messagesFlow(convId).collect { msgs ->
                    _chatState.value = _chatState.value.copy(
                        messages = msgs,
                        hasMoreMessages = msgs.size >= com.novaproject.novai.data.repository.MESSAGES_PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(error = e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun loadMoreMessages() {
        val convId = currentConvId ?: return
        val oldest = _chatState.value.messages.firstOrNull()?.timestamp ?: return
        if (_chatState.value.isLoadingMore) return
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoadingMore = true)
            val older = repo.loadOlderMessages(convId, oldest)
            _chatState.value = _chatState.value.copy(
                messages = older + _chatState.value.messages,
                hasMoreMessages = older.size >= com.novaproject.novai.data.repository.MESSAGES_PAGE_SIZE,
                isLoadingMore = false
            )
        }
    }

    fun resetChat() {
        messagesJob?.cancel(); messagesJob = null; currentConvId = null
        _chatState.value = ChatUiState(
            currentModel = _chatState.value.currentModel,
            aiName = _chatState.value.aiName,
            aiAvatarEmoji = _chatState.value.aiAvatarEmoji,
            accentColor = _chatState.value.accentColor,
            customModels = _chatState.value.customModels
        )
    }

    // ── Full-text search ───────────────────────────────────────────────────────

    fun searchMessages(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        searchJob?.cancel()
        if (query.length < 2) {
            _searchState.value = _searchState.value.copy(isSearching = false, messageResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            // Bug-4: debounce — wait 300 ms after the user stops typing before
            // hitting Firestore, so each keystroke doesn't spawn a full scan.
            kotlinx.coroutines.delay(300)
            _searchState.value = _searchState.value.copy(isSearching = true)
            val results = repo.searchMessages(query)
            _searchState.value = _searchState.value.copy(isSearching = false, messageResults = results)
        }
    }

    fun clearSearch() { searchJob?.cancel(); _searchState.value = SearchUiState() }

    // ── Send / edit ────────────────────────────────────────────────────────────

    fun createNewConversation(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _convState.value = _convState.value.copy(isLoading = true, error = null)
            try {
                val id = repo.createConversation()
                _convState.value = _convState.value.copy(isLoading = false)
                onCreated(id)
            } catch (e: Exception) {
                val msg = e.message ?: "Не удалось создать чат"
                _convState.value = _convState.value.copy(
                    isLoading = false,
                    error = if (msg.contains("PERMISSION_DENIED")) "Нет доступа. Обновите правила Firestore." else msg
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val convId = currentConvId ?: return
        if (text.isBlank() || _chatState.value.isSending) return
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isSending = true, error = null, streamingContent = "")
            try {
                repo.sendMessage(convId, text.trim(), _chatState.value.messages, currentSettings) { chunk ->
                    _chatState.update { it.copy(streamingContent = it.streamingContent + chunk) }
                }
                _chatState.value = _chatState.value.copy(isSending = false, streamingContent = "")
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isSending = false, streamingContent = "",
                    error = e.message ?: "Ошибка отправки"
                )
            }
        }
    }


      fun regenerateLastResponse() {
          val convId = currentConvId ?: return
          if (_chatState.value.isSending) return
          val messages = _chatState.value.messages
          val lastAiIdx = messages.indexOfLast { it.role == "assistant" }
          if (lastAiIdx < 0) return
          val lastUserIdx = messages.subList(0, lastAiIdx).indexOfLast { it.role == "user" }
          if (lastUserIdx < 0) return
          val lastUserMsg = messages[lastUserIdx]
          val historyBefore = if (lastUserIdx > 0) messages.subList(0, lastUserIdx) else emptyList()
          viewModelScope.launch {
              _chatState.value = _chatState.value.copy(isSending = true, error = null, streamingContent = "")
              try {
                  repo.editMessage(convId, lastUserMsg.id, lastUserMsg.content, historyBefore, currentSettings) { chunk ->
                      _chatState.update { it.copy(streamingContent = it.streamingContent + chunk) }
                  }
                  _chatState.value = _chatState.value.copy(isSending = false, streamingContent = "")
              } catch (e: Exception) {
                  _chatState.value = _chatState.value.copy(isSending = false, streamingContent = "", error = e.message ?: "Ошибка")
              }
          }
      }
  
    fun editMessage(fromMessageId: String, newText: String) {
        val convId = currentConvId ?: return
        if (newText.isBlank() || _chatState.value.isSending) return
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isSending = true, error = null, streamingContent = "")
            try {
                val idx = _chatState.value.messages.indexOfFirst { it.id == fromMessageId }
                val before = if (idx > 0) _chatState.value.messages.subList(0, idx) else emptyList()
                repo.editMessage(convId, fromMessageId, newText.trim(), before, currentSettings) { chunk ->
                    _chatState.update { it.copy(streamingContent = it.streamingContent + chunk) }
                }
                _chatState.value = _chatState.value.copy(isSending = false, streamingContent = "")
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isSending = false, streamingContent = "",
                    error = e.message ?: "Ошибка редактирования"
                )
            }
        }
    }

    // ── Conversations management ───────────────────────────────────────────────

    fun renameConversation(convId: String, newTitle: String) {
        viewModelScope.launch { try { repo.renameConversation(convId, newTitle) } catch (_: Exception) {} }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch { try { repo.deleteConversation(convId) } catch (_: Exception) {} }
    }

    /**
     * Toggles pin state with an optimistic UI update so the change is
     * reflected immediately, then confirmed (or rolled back) by Firestore.
     */
    fun pinConversation(convId: String, isPinned: Boolean) {
        // Optimistic: update local state right away
        _convState.value = _convState.value.copy(
            conversations = _convState.value.conversations.map { c ->
                if (c.id == convId) c.copy(isPinned = isPinned) else c
            }
        )
        viewModelScope.launch {
            try {
                repo.pinConversation(convId, isPinned)
            } catch (e: Exception) {
                // Revert on failure
                _convState.value = _convState.value.copy(
                    conversations = _convState.value.conversations.map { c ->
                        if (c.id == convId) c.copy(isPinned = !isPinned) else c
                    },
                    error = e.message ?: "Ошибка закрепления"
                )
            }
        }
    }

    fun addReaction(convId: String, msgId: String, isThumbsUp: Boolean) {
        // Optimistic local toggle so the UI responds instantly
        _chatState.value = _chatState.value.copy(
            messages = _chatState.value.messages.map { msg ->
                if (msg.id != msgId) return@map msg
                if (isThumbsUp) {
                    msg.copy(thumbsUp = if (msg.thumbsUp > 0) 0 else 1, thumbsDown = 0)
                } else {
                    msg.copy(thumbsDown = if (msg.thumbsDown > 0) 0 else 1, thumbsUp = 0)
                }
            }
        )
        viewModelScope.launch { try { repo.toggleReaction(convId, msgId, isThumbsUp) } catch (_: Exception) {} }
    }

    fun switchModel(model: String) {
        currentSettings = currentSettings.copy(customModel = model)
        _chatState.value = _chatState.value.copy(currentModel = model)
    }

    fun getShareText(title: String): String {
        val aiName = currentSettings.aiName.ifBlank { "NovAI" }
        return buildString {
            appendLine("=== $title ==="); appendLine("Диалог из NovAI · Nova Project"); appendLine()
            _chatState.value.messages.forEach { msg ->
                appendLine("[${if (msg.role == "user") "Я" else aiName}]: ${msg.content}"); appendLine()
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try { repo.deleteAccount(); onSuccess() }
            catch (e: Exception) { _convState.value = _convState.value.copy(error = e.message ?: "Ошибка удаления аккаунта") }
        }
    }

    fun clearChatError() { _chatState.value = _chatState.value.copy(error = null) }
    fun clearConvError() { _convState.value = _convState.value.copy(error = null) }
    fun reloadSettings() { subscribeSettings() }
}
