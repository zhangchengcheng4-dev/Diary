package com.vibecoding.data.repository

import android.content.Context
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.local.SyncStatus
import com.vibecoding.data.local.db.AppDatabase
import com.vibecoding.data.local.entity.AudioAssetEntity
import com.vibecoding.data.local.entity.DiaryEntryEntity
import com.vibecoding.data.local.entity.SyncStateEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class LocalDiaryRepository(
    context: Context
) {
    private val db = AppDatabase.getInstance(context.applicationContext)

    fun observeEntry(entryId: String): Flow<DiaryEntryEntity?> {
        return db.diaryEntryDao().observeById(entryId)
    }

    fun observeActiveEntriesByUser(userId: String): Flow<List<DiaryEntryEntity>> {
        return db.diaryEntryDao().observeActiveByUser(userId)
    }

    suspend fun findEntry(entryId: String): DiaryEntryEntity? {
        return db.diaryEntryDao().findById(entryId)
    }

    suspend fun findRecoverableProcessingEntries(): List<DiaryEntryEntity> {
        return db.diaryEntryDao().findByProcessingStatus(DiaryProcessingStatus.RecordedPendingUpload) +
            db.diaryEntryDao().findByProcessingStatus(DiaryProcessingStatus.Processing)
    }

    suspend fun findSyncState(syncStateId: String): SyncStateEntity? {
        return db.syncStateDao().findById(syncStateId)
    }

    suspend fun findAudioAssets(entryId: String): List<AudioAssetEntity> {
        return db.audioAssetDao().findAllByEntryId(entryId)
    }

    suspend fun saveDetailEdits(
        entryId: String,
        title: String,
        polishedArticle: String,
        category: String,
        tagsText: String,
        entryDateLocal: String
    ): String? {
        val entry = db.diaryEntryDao().findById(entryId) ?: return "日记不存在"
        val date = runCatching { LocalDate.parse(entryDateLocal.trim()) }.getOrNull()
            ?: return "日期格式应为 yyyy-MM-dd"
        val now = toIso8601Utc(nowUtcMillis())
        db.diaryEntryDao().update(
            entry.copy(
                title = title.trim().ifBlank { "语音日记" },
                polishedArticle = polishedArticle.trim(),
                primaryCategoryId = normalizeCategory(category),
                dynamicTags = normalizeTags(tagsText).joinToString(","),
                entryOccurredAt = rebuildOccurredAt(entry.entryOccurredAt.ifBlank { entry.createdAt }, date),
                entryDateLocal = date.toString(),
                updatedAt = now
            )
        )
        return null
    }

    suspend fun softDeleteEntry(entryId: String) {
        val now = toIso8601Utc(nowUtcMillis())
        val entry = db.diaryEntryDao().findById(entryId)
        db.diaryEntryDao().softDelete(entryId = entryId, deletedAt = now, updatedAt = now)
        entry?.let {
            db.syncStateDao().findById(it.syncStateId)?.let { sync ->
                db.syncStateDao().upsert(
                    sync.copy(
                        syncStatus = SyncStatus.Deleted,
                        lastErrorCode = null,
                        lastErrorMessage = null,
                        updatedAt = now
                    )
                )
            }
        }
    }

    private fun rebuildOccurredAt(originalUtc: String, newDate: LocalDate): String {
        val zone = ZoneId.systemDefault()
        return runCatching {
            val originalLocal = Instant.parse(originalUtc).atZone(zone)
            newDate
                .atTime(originalLocal.toLocalTime())
                .atZone(zone)
                .toInstant()
                .toString()
        }.getOrElse {
            newDate.atStartOfDay(zone).toInstant().toString()
        }
    }

    private fun normalizeCategory(category: String): String {
        val c = category.trim().lowercase()
        return if (c in ALLOWED_CATEGORIES) c else "life"
    }

    private fun normalizeTags(tagsText: String): List<String> {
        return tagsText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(5)
    }

    private companion object {
        private val ALLOWED_CATEGORIES = setOf("work", "study", "life", "emotion", "health")
    }
}
