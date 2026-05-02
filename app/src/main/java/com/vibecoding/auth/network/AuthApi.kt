package com.vibecoding.auth.network

import com.vibecoding.auth.model.AuthErrorCode
import com.vibecoding.auth.model.AuthException
import com.vibecoding.auth.model.AuthRequest
import com.vibecoding.auth.model.AuthResponse
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import java.io.IOException
import java.util.UUID

object AuthEndpoints {
    const val BASE_URL = "https://dev-staging-placeholder.vibecoding.local"
    const val LOGIN_PATH = "/login"
    const val REGISTER_PATH = "/register"
}

interface AuthApi {
    suspend fun login(request: AuthRequest): AuthResponse
    suspend fun register(request: AuthRequest): AuthResponse
}

class FakeAuthApi : AuthApi {
    override suspend fun login(request: AuthRequest): AuthResponse {
        when {
            request.email.contains("network", ignoreCase = true) -> throw IOException("Network error")
            request.email.contains("server", ignoreCase = true) -> throw AuthException(AuthErrorCode.SERVER_ERROR)
            request.email.contains("nouser", ignoreCase = true) -> throw AuthException(AuthErrorCode.USER_NOT_FOUND)
            request.email.contains("invalid", ignoreCase = true) -> throw AuthException(AuthErrorCode.INVALID_CREDENTIALS)
            request.password.length < 8 -> throw AuthException(AuthErrorCode.INVALID_CREDENTIALS)
        }
        return issueSession()
    }

    override suspend fun register(request: AuthRequest): AuthResponse {
        when {
            request.email.contains("network", ignoreCase = true) -> throw IOException("Network error")
            request.email.contains("server", ignoreCase = true) -> throw AuthException(AuthErrorCode.SERVER_ERROR)
            request.email.contains("exists", ignoreCase = true) -> throw AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED)
            request.password.length < 8 -> throw AuthException(AuthErrorCode.INVALID_CREDENTIALS)
        }
        return issueSession()
    }

    private fun issueSession(): AuthResponse {
        val expiresAt = nowUtcMillis() + 24L * 60 * 60 * 1000
        return AuthResponse(
            sessionToken = UUID.randomUUID().toString(),
            sessionExpiresAt = toIso8601Utc(expiresAt)
        )
    }
}

