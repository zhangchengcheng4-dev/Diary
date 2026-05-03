package com.vibecoding.data.repository

import com.vibecoding.data.network.SyncPullResponse
import com.vibecoding.data.network.SyncPushRequest
import com.vibecoding.data.network.SyncPushResponse

interface SyncRepository {
    suspend fun push(request: SyncPushRequest): Result<SyncPushResponse>
    suspend fun pull(cursor: String?): Result<SyncPullResponse>
}
