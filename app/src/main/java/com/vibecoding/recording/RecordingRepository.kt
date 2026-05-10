package com.vibecoding.recording

import android.content.Context
import android.media.MediaMetadataRetriever
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import com.vibecoding.data.local.DiaryProcessingStatus
import com.vibecoding.data.local.SyncStatus
import com.vibecoding.data.local.db.AppDatabase
import com.vibecoding.data.local.entity.AudioAssetEntity
import com.vibecoding.data.local.entity.DiaryEntryEntity
import com.vibecoding.data.local.entity.SyncStateEntity
import java.io.File
import java.time.LocalDate
import java.util.UUID

class RecordingRepository(
    context: Context
) {
    companion object {
        const val MVP_LOCAL_USER_ID: String = "mvp_local_user"
    }

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val recordingsDir = File(appContext.filesDir, "recordings")

    suspend fun createDraft(userId: String): String {
        val entryId = UUID.randomUUID().toString()
        val syncStateId = UUID.randomUUID().toString()
        val now = toIso8601Utc(nowUtcMillis())
        val dateLocal = LocalDate.now().toString()
        db.diaryEntryDao().upsert(
            DiaryEntryEntity(
                entryId = entryId,
                userId = userId,
                entryOccurredAt = now,
                entryDateLocal = dateLocal,
                title = "Voice Draft",
                rawTranscript = "",
                polishedArticle = "",
                primaryCategoryId = "life",
                dynamicTags = "",
                processingStatus = DiaryProcessingStatus.DraftRecording,
                audioAssetId = "",
                syncStateId = syncStateId,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
        )
        db.syncStateDao().upsert(
            SyncStateEntity(
                syncStateId = syncStateId,
                entityType = "DiaryEntry",
                entityId = entryId,
                userId = userId,
                syncStatus = SyncStatus.PendingUpload,
                retryCount = 0,
                lastErrorCode = null,
                lastErrorMessage = null,
                lastAttemptAt = null,
                nextRetryAt = null,
                createdAt = now,
                updatedAt = now
            )
        )
        return entryId
    }

    suspend fun saveSegmentsAndFinalize(
        userId: String,
        entryId: String,
        segments: List<RecordingSegment>
    ): RecordingDraftResult {
        val now = toIso8601Utc(nowUtcMillis())
        var totalDurationMs = 0L
        var primaryAudioAssetId = ""
        segments.forEachIndexed { index, segment ->
            if (index == 0) primaryAudioAssetId = segment.audioAssetId
            totalDurationMs += segment.durationMs
            db.audioAssetDao().upsert(
                AudioAssetEntity(
                    audioAssetId = segment.audioAssetId,
                    entryId = entryId,
                    userId = userId,
                    localPath = segment.localPath,
                    remoteUrl = null,
                    durationMs = segment.durationMs,
                    mimeType = segment.mimeType,
                    fileSizeBytes = segment.fileSizeBytes,
                    checksumSha256 = null,
                    uploadStatus = "pending_upload",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val existing = db.diaryEntryDao().findById(entryId) ?: error("Draft entry not found: $entryId")
        db.diaryEntryDao().upsert(
            existing.copy(
                processingStatus = DiaryProcessingStatus.RecordedPendingUpload,
                audioAssetId = primaryAudioAssetId,
                updatedAt = now
            )
        )

        return RecordingDraftResult(
            entryId = entryId,
            totalDurationMs = totalDurationMs,
            segments = segments
        )
    }

    suspend fun clearDraftAndFiles(entryId: String) {
        val assets = db.audioAssetDao().findAllByEntryId(entryId)
        assets.forEach { asset ->
            runCatching { File(asset.localPath).delete() }
        }
        db.audioAssetDao().deleteByEntryId(entryId)
        db.syncStateDao().deleteByEntity(entityType = "DiaryEntry", entityId = entryId)
        db.diaryEntryDao().deleteById(entryId)
    }

    suspend fun recoverOrCleanupRecordingsOnAppStart() {
        recordingsDir.mkdirs()
        val files = recordingsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "m4a" } ?: emptyArray()
        val allAssets = db.audioAssetDao().findAll()
        val knownPathSet = allAssets.map { it.localPath }.toSet()

        files.forEach { file ->
            val absolutePath = file.absolutePath
            val size = file.length()
            if (size <= 0L) {
                runCatching { file.delete() }
                db.audioAssetDao().deleteByLocalPath(absolutePath)
                return@forEach
            }

            if (knownPathSet.contains(absolutePath)) {
                return@forEach
            }

            val now = toIso8601Utc(nowUtcMillis())
            val entryId = UUID.randomUUID().toString()
            val syncStateId = UUID.randomUUID().toString()
            val audioAssetId = UUID.randomUUID().toString()
            val recoveredDurationMs = extractDurationMs(file)

            db.diaryEntryDao().upsert(
                DiaryEntryEntity(
                    entryId = entryId,
                    userId = MVP_LOCAL_USER_ID,
                    entryOccurredAt = now,
                    entryDateLocal = LocalDate.now().toString(),
                    title = "Recovered Voice Draft",
                    rawTranscript = "",
                polishedArticle = "",
                primaryCategoryId = "life",
                dynamicTags = "",
                processingStatus = DiaryProcessingStatus.RecordedPendingUpload,
                    audioAssetId = audioAssetId,
                    syncStateId = syncStateId,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null
                )
            )
            db.audioAssetDao().upsert(
                AudioAssetEntity(
                    audioAssetId = audioAssetId,
                    entryId = entryId,
                    userId = MVP_LOCAL_USER_ID,
                    localPath = absolutePath,
                    remoteUrl = null,
                    durationMs = recoveredDurationMs,
                    mimeType = "audio/mp4",
                    fileSizeBytes = size,
                    checksumSha256 = null,
                    uploadStatus = "pending_upload",
                    createdAt = now,
                    updatedAt = now
                )
            )
            db.syncStateDao().upsert(
                SyncStateEntity(
                    syncStateId = syncStateId,
                    entityType = "DiaryEntry",
                    entityId = entryId,
                    userId = MVP_LOCAL_USER_ID,
                    syncStatus = SyncStatus.PendingUpload,
                    retryCount = 0,
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    lastAttemptAt = null,
                    nextRetryAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val draftEntries = db.diaryEntryDao().findByProcessingStatus(DiaryProcessingStatus.DraftRecording)
        draftEntries.forEach { draft ->
            val assets = db.audioAssetDao().findAllByEntryId(draft.entryId)
            val hasValidFile = assets.any { asset ->
                val f = File(asset.localPath)
                f.exists() && f.length() > 0L
            }
            if (!hasValidFile) {
                db.syncStateDao().deleteByEntity(entityType = "DiaryEntry", entityId = draft.entryId)
                db.audioAssetDao().deleteByEntryId(draft.entryId)
                db.diaryEntryDao().deleteById(draft.entryId)
            }
        }
    }

    private fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }.getOrDefault(0L).coerceAtLeast(0L).also {
            runCatching { retriever.release() }
        }
    }
}
