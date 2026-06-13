package com.novaproject.novai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novaproject.novai.data.model.UserStats
import com.novaproject.novai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val stats: UserStats = UserStats(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val stats = repo.getUserStats()
                _state.value = StatsUiState(stats = stats, isLoading = false)
            } catch (e: Exception) {
                _state.value = StatsUiState(isLoading = false, error = e.message ?: "Ошибка загрузки")
            }
        }
    }
}
