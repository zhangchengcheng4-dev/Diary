package com.vibecoding.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_assets",
    indices = [Index(value = ["entryId"], unique = true)]
)
data class AudioAssetEntity(
    @PrimaryKey val audioAssetId: String,
    val entryId: String,
    val userId: String,
    val localPath: String,
    val remoteUrl: String?,
    val durationMs: Long,
    val mimeType: String,
    val fileSizeBytes: Long,
    val checksumSha256: String?,
    val uploadStatus: String,
    val createdAt: String,
    val updatedAt: String
)
