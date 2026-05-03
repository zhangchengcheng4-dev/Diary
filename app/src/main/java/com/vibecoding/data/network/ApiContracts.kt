package com.vibecoding.data.network

data class ApiEnvelope<T>(
    val requestId: String,
    val serverTime: String,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val retryable: Boolean
)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

object V1Endpoints {
    const val AUTH_LOGIN = "/v1/auth/login"
    const val AUDIO_UPLOAD = "/v1/audio/upload"
    const val AUDIO_JOB = "/v1/audio/jobs/{jobId}"
    const val DIARIES = "/v1/diaries"
    const val DIARY_DETAIL = "/v1/diaries/{entryId}"
    const val SYNC_PUSH = "/v1/sync/push"
    const val SYNC_PULL = "/v1/sync/pull"
}
