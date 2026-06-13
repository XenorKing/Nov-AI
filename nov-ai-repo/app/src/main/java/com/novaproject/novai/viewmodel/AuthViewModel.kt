package com.novaproject.novai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.novaproject.novai.data.repository.AuthRepository
import com.novaproject.novai.data.repository.PromoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: FirebaseUser? = null,
    val successMessage: String? = null,
    val premiumExpiresAt: Long = 0L,
    val promoLoading: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val promoRepo: PromoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.authState.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
        viewModelScope.launch {
            promoRepo.premiumFlow().collect { expiresAt ->
                _uiState.value = _uiState.value.copy(premiumExpiresAt = expiresAt)
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполните все поля")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.signIn(email.trim(), password).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it.message)) }
            )
        }
    }

    fun register(email: String, password: String, name: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполните все поля")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Пароли не совпадают")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Пароль минимум 6 символов")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.register(email.trim(), password, name.trim()).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it.message)) }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            repo.resetPassword(email).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Письмо отправлено на $email") },
                onFailure = { _uiState.value = _uiState.value.copy(error = friendlyError(it.message)) }
            )
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите имя")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.updateDisplayName(newName.trim()).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Имя обновлено") },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it.message)) }
            )
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполните все поля")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Пароли не совпадают")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Новый пароль минимум 6 символов")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.updatePassword(currentPassword, newPassword).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Пароль изменён") },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it.message)) }
            )
        }
    }

    fun redeemPromoCode(code: String) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите промокод")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(promoLoading = true, error = null)
            promoRepo.redeemCode(code).fold(
                onSuccess = { duration ->
                    _uiState.value = _uiState.value.copy(promoLoading = false, successMessage = "Активировано: Premium на $duration!")
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(promoLoading = false, error = it.message ?: "Ошибка активации")
                }
            )
        }
    }

    fun signOut() = repo.signOut()
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(successMessage = null) }

    private fun friendlyError(msg: String?): String = when {
        msg == null -> "Неизвестная ошибка"
        msg.contains("no user record") || msg.contains("user-not-found") -> "Пользователь не найден"
        msg.contains("password is invalid") || msg.contains("wrong-password") || msg.contains("INVALID_LOGIN_CREDENTIALS") -> "Неверный пароль"
        msg.contains("email address is already") || msg.contains("email-already-in-use") -> "Email уже используется"
        msg.contains("badly formatted") || msg.contains("invalid-email") -> "Неверный формат email"
        msg.contains("network") -> "Ошибка сети"
        msg.contains("requires-recent-login") -> "Требуется повторный вход"
        else -> msg
    }
}
