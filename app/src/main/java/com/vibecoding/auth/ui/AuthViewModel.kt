package com.vibecoding.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecoding.app.BuildConfig
import com.vibecoding.auth.data.AuthRepository
import com.vibecoding.auth.model.AuthErrorCode
import com.vibecoding.auth.model.AuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER
}

enum class SessionBootstrapState {
    CHECKING,
    AUTHENTICATED,
    UNAUTHENTICATED
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorCode: AuthErrorCode? = null,
    val sessionState: SessionBootstrapState = SessionBootstrapState.CHECKING
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        bootstrapSession()
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorCode = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorCode = null) }
    }

    fun switchMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorCode = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorCode = null) }
                repository.createDebugMockSession()
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                sessionState = SessionBootstrapState.AUTHENTICATED
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val code = (throwable as? AuthException)?.code ?: AuthErrorCode.SERVER_ERROR
                        _uiState.update { it.copy(isLoading = false, errorCode = code) }
                    }
            }
            return
        }
        if (current.email.isBlank() || current.password.length < 8) {
            _uiState.update { it.copy(errorCode = AuthErrorCode.INVALID_CREDENTIALS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorCode = null) }
            val result = when (current.mode) {
                AuthMode.LOGIN -> repository.login(current.email, current.password)
                AuthMode.REGISTER -> repository.register(current.email, current.password)
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionState = SessionBootstrapState.AUTHENTICATED
                        )
                    }
                }
                .onFailure { throwable ->
                    val code = (throwable as? AuthException)?.code ?: AuthErrorCode.SERVER_ERROR
                    _uiState.update { it.copy(isLoading = false, errorCode = code) }
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.update {
            it.copy(
                sessionState = SessionBootstrapState.UNAUTHENTICATED,
                password = "",
                errorCode = null
            )
        }
    }

    private fun bootstrapSession() {
        val isValid = repository.isSessionValidNow()
        _uiState.update {
            it.copy(
                sessionState = if (isValid) {
                    SessionBootstrapState.AUTHENTICATED
                } else {
                    SessionBootstrapState.UNAUTHENTICATED
                }
            )
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository) as T
    }
}
