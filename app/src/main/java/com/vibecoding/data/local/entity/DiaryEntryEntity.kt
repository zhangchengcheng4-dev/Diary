package com.vibecoding.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val entryId: String,
    val userId: String,
    val entryOccurredAt: String,
    val entryDateLocal: String,
    val title: String,
    val rawTranscript: String,
    val polishedArticle: String,
    val primaryCategoryId: String,
    val dynamicTags: String,
    val processingStatus: String,
    val audioAssetId: String,
    val syncStateId: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)
