package com.vibecoding.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_states")
data class SyncStateEntity(
    @PrimaryKey val syncStateId: String,
    val entityType: String,
    val entityId: String,
    val userId: String,
    val syncStatus: String,
    val retryCount: Int,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val lastAttemptAt: String?,
    val nextRetryAt: String?,
    val createdAt: String,
    val updatedAt: String
)
