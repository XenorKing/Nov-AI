package com.novaproject.novai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novaproject.novai.data.model.AISettings
import com.novaproject.novai.data.model.FREE_MODEL_LIST
import com.novaproject.novai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AISettings = AISettings(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    /** Which model's per-model prompt is currently being edited in the UI. */
    val promptEditModelId: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.settingsFlow()
                .catch { /* keep current state */ }
                .collect { settings ->
                    if (settings != _state.value.settings && !_state.value.isSaving) {
                        _state.value = _state.value.copy(settings = settings)
                    }
                }
        }
    }

    private fun update(block: AISettings.() -> AISettings) {
        _state.value = _state.value.copy(settings = block(_state.value.settings), saved = false)
    }

    fun updateTemperature(v: Float) = update { copy(temperature = v) }
    fun updateMaxTokens(v: Int) = update { copy(maxTokens = v) }
    fun updateTopP(v: Float) = update { copy(topP = v) }
    fun updateFrequencyPenalty(v: Float) = update { copy(frequencyPenalty = v) }
    fun updatePresencePenalty(v: Float) = update { copy(presencePenalty = v) }
    fun updateSendHistory(v: Boolean) = update { copy(sendHistory = v) }
    fun updateSystemPromptOverride(v: String) = update { copy(systemPromptOverride = v) }
    fun updateCustomModel(v: String) = update { copy(customModel = v) }
    fun updateOpenRouterToken(v: String) = update { copy(openRouterToken = v) }
    fun updateAiName(v: String) = update { copy(aiName = v) }
    fun updateAiAvatarEmoji(v: String) = update { copy(aiAvatarEmoji = v) }
    fun updateAccentColor(v: String) = update { copy(accentColor = v) }

    /** Set which model's prompt is being edited in the settings UI. */
    fun setPromptEditModel(modelId: String) {
        _state.value = _state.value.copy(promptEditModelId = modelId)
    }

    /**
     * Update the system prompt for a specific model.
     * Uses [FREE_MODEL_LIST] IDs plus custom model IDs as keys.
     * An empty [prompt] removes the entry from the map.
     */
    fun addCustomModel(modelId: String) = update {
        val id = modelId.trim()
        if (id.isNotBlank() && !customModels.contains(id)) copy(customModels = customModels + id) else this
    }

    fun removeCustomModel(modelId: String) = update {
        copy(customModels = customModels.filter { it != modelId })
    }

    fun updateModelPrompt(modelId: String, prompt: String) = update {
        val newMap = modelPrompts.toMutableMap()
        if (prompt.isBlank()) newMap.remove(modelId) else newMap[modelId] = prompt
        copy(modelPrompts = newMap)
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                repo.saveSettings(_state.value.settings)
                _state.value = _state.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Ошибка")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
