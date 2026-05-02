package com.vibecoding.auth.model

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val sessionToken: String,
    val sessionExpiresAt: String
)

enum class AuthErrorCode {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    EMAIL_ALREADY_REGISTERED,
    SERVER_ERROR,
    NETWORK_ERROR
}

class AuthException(val code: AuthErrorCode) : Exception(code.name)

