package com.vibecoding.auth.data

import com.vibecoding.app.BuildConfig
import com.vibecoding.auth.model.AuthErrorCode
import com.vibecoding.auth.model.AuthException
import com.vibecoding.auth.network.AuthApi
import com.vibecoding.auth.model.AuthRequest
import com.vibecoding.auth.session.SessionStore
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import java.io.IOException

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) {
    suspend fun login(email: String, password: String): Result<Unit> {
        return runAuthCall { authApi.login(AuthRequest(email, password)) }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        return runAuthCall { authApi.register(AuthRequest(email, password)) }
    }

    fun createDebugMockSession(): Result<Unit> {
        if (!BuildConfig.DEBUG) {
            return Result.failure(AuthException(AuthErrorCode.SERVER_ERROR))
        }
        val sessionExpiresAt = toIso8601Utc(nowUtcMillis() + MOCK_SESSION_VALIDITY_MILLIS)
        sessionStore.saveSession(
            sessionToken = DEBUG_MOCK_SESSION_TOKEN,
            sessionExpiresAt = sessionExpiresAt
        )
        return Result.success(Unit)
    }

    fun isSessionValidNow(): Boolean = sessionStore.isSessionValidNow()

    fun logout() = sessionStore.clear()

    private suspend fun runAuthCall(apiCall: suspend () -> com.vibecoding.auth.model.AuthResponse): Result<Unit> {
        return try {
            val response = apiCall()
            sessionStore.saveSession(
                sessionToken = response.sessionToken,
                sessionExpiresAt = response.sessionExpiresAt
            )
            Result.success(Unit)
        } catch (throwable: Throwable) {
            val mapped = when (throwable) {
                is AuthException -> throwable
                is IOException -> AuthException(AuthErrorCode.NETWORK_ERROR)
                else -> AuthException(AuthErrorCode.SERVER_ERROR)
            }
            Result.failure(mapped)
        }
    }

    private companion object {
        const val DEBUG_MOCK_SESSION_TOKEN = "debug_mock_session"
        const val MOCK_SESSION_VALIDITY_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
