package com.vibecoding.diary

import android.content.Context
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.local.SyncStatus
import com.vibecoding.data.local.db.AppDatabase

class DiaryAssemblyUseCase(
    context: Context,
    private val diaryAiProcessor: DiaryAiProcessor = FakeDiaryAiProcessor()
) {
    companion object {
        private val ALLOWED_CATEGORIES = setOf("work", "study", "life", "emotion", "health")
    }

    private val db = AppDatabase.getInstance(context.applicationContext)

    suspend fun assembleAndSave(
        entryId: String,
        transcript: String,
        category: String? = null,
        tags: List<String>? = null,
        polishedArticle: String? = null
    ): DiaryAssemblyResult {
        val normalizedTranscript = transcript.trim()
        if (normalizedTranscript.isBlank()) {
            return DiaryAssemblyResult.Failed("EMPTY_TRANSCRIPT", "Transcript is empty")
        }

        val entry = db.diaryEntryDao().findById(entryId)
            ?: return DiaryAssemblyResult.Failed("ENTRY_NOT_FOUND", "Diary entry not found")
        val now = toIso8601Utc(nowUtcMillis())
        val aiResult = runCatching { diaryAiProcessor.process(normalizedTranscript) }.getOrNull()
        val normalizedCategory = normalizeCategory(category ?: aiResult?.primaryCategoryId)
        val normalizedTags = normalizeTags(tags ?: aiResult?.dynamicTags, normalizedCategory)
        val finalTitle = aiResult?.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: entry.title.ifBlank { "语音日记" }
        val finalPolishedArticle = polishedArticle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: aiResult?.polishedArticle
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: normalizedTranscript

        db.diaryEntryDao().upsert(
            entry.copy(
                title = finalTitle,
                rawTranscript = normalizedTranscript,
                primaryCategoryId = normalizedCategory,
                dynamicTags = normalizedTags.joinToString(","),
                polishedArticle = finalPolishedArticle,
                processingStatus = DiaryProcessingStatus.ProcessedSucceeded,
                updatedAt = now
            )
        )
        db.syncStateDao().findById(entry.syncStateId)?.let { sync ->
            db.syncStateDao().upsert(
                sync.copy(
                    syncStatus = SyncStatus.PendingUpload,
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    updatedAt = now
                )
            )
        }

        return DiaryAssemblyResult.Saved(entryId = entryId)
    }

    private fun normalizeCategory(category: String?): String {
        val c = category?.trim()?.lowercase() ?: "life"
        return if (c in ALLOWED_CATEGORIES) c else "life"
    }

    private fun normalizeTags(tags: List<String>?, category: String): List<String> {
        return tags.orEmpty()
            .map { normalizeTag(it) }
            .filter { it.isNotEmpty() }
            .filterNot { isCategoryId(it) }
            .filterNot { it == category }
            .distinct()
            .take(5)
    }

    private fun normalizeTag(tag: String): String {
        return when (tag.trim().lowercase()) {
            "work", "工作" -> "工作"
            "study", "学习" -> "学习"
            "life", "生活" -> "生活"
            "emotion", "情绪" -> "情绪"
            "health", "健康" -> "健康"
            else -> tag.trim().lowercase()
        }
    }

    private fun isCategoryId(tag: String): Boolean {
        return tag in ALLOWED_CATEGORIES
    }
}

sealed interface DiaryAssemblyResult {
    data class Saved(val entryId: String) : DiaryAssemblyResult
    data class Failed(val code: String, val message: String) : DiaryAssemblyResult
}
