package com.vibecoding.diary

import android.content.Context
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.local.db.AppDatabase

class DiaryAssemblyUseCase(
    context: Context
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
        val normalizedCategory = normalizeCategory(category)
        val normalizedTags = normalizeTags(tags)

        // Temporary AI placeholder: until classification/tagging/polishing is implemented,
        // the polished article remains the ASR transcript and tags may be empty.
        val finalPolishedArticle = polishedArticle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: normalizedTranscript

        db.diaryEntryDao().upsert(
            entry.copy(
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
                    syncStatus = DiaryProcessingStatus.ProcessedSucceeded,
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

    private fun normalizeTags(tags: List<String>?): List<String> {
        return tags.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(5)
    }
}

sealed interface DiaryAssemblyResult {
    data class Saved(val entryId: String) : DiaryAssemblyResult
    data class Failed(val code: String, val message: String) : DiaryAssemblyResult
}
