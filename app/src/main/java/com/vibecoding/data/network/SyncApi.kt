package com.vibecoding.data.network

data class SyncPushItem(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val clientUpdatedAt: String
)

data class SyncPushRequest(
    val userId: String,
    val items: List<SyncPushItem>
)

data class SyncPushResponse(
    val acceptedIds: List<String>,
    val rejectedIds: List<String>
)

data class SyncPullItem(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val serverUpdatedAt: String
)

data class SyncPullResponse(
    val items: List<SyncPullItem>,
    val nextCursor: String?
)

interface SyncApi {
    suspend fun push(request: SyncPushRequest): ApiEnvelope<SyncPushResponse>
    suspend fun pull(cursor: String?): ApiEnvelope<SyncPullResponse>
}
