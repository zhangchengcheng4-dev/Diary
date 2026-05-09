package com.vibecoding.recording

import android.content.Context
import com.vibecoding.auth.util.nowUtcMillis
import com.vibecoding.auth.util.toIso8601Utc
import com.vibecoding.app.BuildConfig
import com.vibecoding.data.local.db.AppDatabase
import com.vibecoding.data.local.entity.SyncStateEntity
import com.vibecoding.data.network.AudioApi
import com.vibecoding.data.network.MockAudioApi
import com.vibecoding.data.network.UploadAudioRequest
import com.vibecoding.data.network.XfyunAudioApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class Step9ProcessingUseCase(
    context: Context,
    private val audioApi: AudioApi = createAudioApi()
) {
    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_POLL_COUNT = 12
        private val ALLOWED_CATEGORIES = setOf("work", "study", "life", "emotion", "health")
        private fun createAudioApi(): AudioApi {
            return if (BuildConfig.XFYUN_USE_MOCK) {
                MockAudioApi()
            } else {
                XfyunAudioApi(
                    appId = BuildConfig.XFYUN_APP_ID,
                    apiKey = BuildConfig.XFYUN_API_KEY,
                    apiSecret = BuildConfig.XFYUN_API_SECRET
                )
            }
        }
    }

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val apiName = audioApi::class.simpleName ?: "UnknownAudioApi"

    suspend fun run(entryId: String) {
        runCatching {
            withContext(Dispatchers.IO) { writeDebug(entryId, "Step9 start api=$apiName") }
            if (!BuildConfig.XFYUN_USE_MOCK &&
                (BuildConfig.XFYUN_APP_ID.isBlank() || BuildConfig.XFYUN_API_KEY.isBlank() || BuildConfig.XFYUN_API_SECRET.isBlank())
            ) {
                withContext(Dispatchers.IO) {
                    markFailed(entryId, "XFYUN_CONFIG_MISSING", "xfyun WebSocket appId/apiKey/apiSecret missing and mock disabled")
                }
                return
            }
            val entry = withContext(Dispatchers.IO) { db.diaryEntryDao().findById(entryId) } ?: return
            val assets = withContext(Dispatchers.IO) { db.audioAssetDao().findAllByEntryId(entryId) }
            if (assets.isEmpty()) {
                markFailed(entryId, "AUDIO_NOT_FOUND", "No audio assets for entry")
                return
            }
            withContext(Dispatchers.IO) { writeDebug(entryId, "assets=${assets.size}") }
            withContext(Dispatchers.IO) {
                val a = assets.first()
                writeDebug(
                    entryId,
                    "asset audioAssetId=${a.audioAssetId} path=${a.localPath} exists=${File(a.localPath).exists()} size=${File(a.localPath).length()} mime=${a.mimeType} durationMs=${a.durationMs}"
                )
            }

            val durationSeconds = ((assets.sumOf { it.durationMs } / 1000L).coerceAtLeast(1L)).toInt()
            withContext(Dispatchers.IO) { writeDebug(entryId, "before set processing") }
            withContext(Dispatchers.IO) {
                withTimeout(5_000) {
                    updateProcessingStatus(entry, "processing")
                }
            }
            withContext(Dispatchers.IO) { writeDebug(entryId, "asr start duration=$durationSeconds segmentCount=${assets.size}") }

            val transcriptParts = mutableListOf<String>()
            for ((index, asset) in assets.withIndex()) {
                val part = processAsrSegment(
                    entryId = entryId,
                    userId = entry.userId,
                    file = File(asset.localPath),
                    durationSeconds = ((asset.durationMs / 1000L).coerceAtLeast(1L)).toInt(),
                    segmentIndex = index + 1,
                    segmentCount = assets.size
                ) ?: return
                if (part.isNotBlank()) transcriptParts += part
            }

            val finalTranscript = transcriptParts.joinToString(separator = "\n").trim()
            if (finalTranscript.isBlank()) {
                withContext(Dispatchers.IO) { markFailed(entryId, "ASR_EMPTY_TRANSCRIPT", "ASR returned empty transcript") }
                return
            }

            // Temporary AI placeholder: Step 9 only validates ASR. Category, tags, and polished article
            // will be replaced by a later AI classification/tagging/polishing step.
            val finalCategory = normalizeCategory(null)
            val finalTags = normalizeTags(emptyList()).joinToString(",")
            val finalPolished = finalTranscript

            withContext(Dispatchers.IO) {
                val updated = db.diaryEntryDao().findById(entryId) ?: return@withContext
                val now = toIso8601Utc(nowUtcMillis())
                db.diaryEntryDao().upsert(
                    updated.copy(
                        rawTranscript = finalTranscript,
                        primaryCategoryId = finalCategory,
                        dynamicTags = finalTags,
                        polishedArticle = finalPolished,
                        processingStatus = "processed_succeeded",
                        updatedAt = now
                    )
                )
                db.syncStateDao().findById(updated.syncStateId)?.let { sync ->
                    db.syncStateDao().upsert(
                        sync.copy(
                            syncStatus = "processed_succeeded",
                            lastErrorCode = null,
                            lastErrorMessage = "Step9 ASR success api=$apiName transcriptLength=${finalTranscript.length}",
                            updatedAt = now
                        )
                    )
                }
            }
        }.onFailure { ex ->
            withContext(Dispatchers.IO) {
                markFailed(entryId, "STEP9_EXCEPTION", ex.stackTraceToString().take(1200))
            }
        }
    }

    private suspend fun processAsrSegment(
        entryId: String,
        userId: String,
        file: File,
        durationSeconds: Int,
        segmentIndex: Int,
        segmentCount: Int
    ): String? {
        if (!file.exists() || file.length() <= 0L) {
            withContext(Dispatchers.IO) {
                markFailed(entryId, "AUDIO_FILE_MISSING", "Audio segment $segmentIndex/$segmentCount is missing")
            }
            return null
        }

        withContext(Dispatchers.IO) { writeDebug(entryId, "asr segment $segmentIndex/$segmentCount start size=${file.length()}") }
        val uploadEnvelope = withContext(Dispatchers.IO) {
            audioApi.uploadAudio(
                UploadAudioRequest(
                    userId = userId,
                    fileName = file.absolutePath,
                    contentType = "audio/mp4",
                    durationSeconds = durationSeconds
                )
            )
        }
        val uploadData = uploadEnvelope.data
        if (uploadData == null) {
            withContext(Dispatchers.IO) {
                markFailed(entryId, uploadEnvelope.error?.code ?: "ASR_START_FAILED", uploadEnvelope.error?.message ?: "ASR start failed")
            }
            return null
        }

        for (i in 0 until MAX_POLL_COUNT) {
            delay(POLL_INTERVAL_MS)
            withContext(Dispatchers.IO) { writeDebug(entryId, "asr segment $segmentIndex/$segmentCount poll ${i + 1}/$MAX_POLL_COUNT") }
            val poll = withContext(Dispatchers.IO) { audioApi.getAudioJob(uploadData.jobId) }
            val job = poll.data
            if (job == null) {
                withContext(Dispatchers.IO) {
                    markFailed(entryId, poll.error?.code ?: "ASR_POLL_FAILED", poll.error?.message ?: "ASR polling failed")
                }
                return null
            }
            if (job.status == "failed") {
                withContext(Dispatchers.IO) { markFailed(entryId, "ASR_FAILED", "ASR segment $segmentIndex/$segmentCount failed") }
                return null
            }
            if (job.status == "succeeded") {
                val transcript = job.transcript.orEmpty()
                withContext(Dispatchers.IO) {
                    writeDebug(entryId, "asr segment $segmentIndex/$segmentCount done transcriptLength=${transcript.length}")
                }
                return transcript
            }
        }

        withContext(Dispatchers.IO) { markFailed(entryId, "ASR_TIMEOUT", "ASR segment $segmentIndex/$segmentCount polling timeout") }
        return null
    }

    private suspend fun updateProcessingStatus(entry: com.vibecoding.data.local.entity.DiaryEntryEntity, status: String) {
        writeDebug(entry.entryId, "update status begin=$status")
        val now = toIso8601Utc(nowUtcMillis())
        writeDebug(entry.entryId, "update diary upsert")
        db.diaryEntryDao().upsert(entry.copy(processingStatus = status, updatedAt = now))
        writeDebug(entry.entryId, "update sync findById")
        db.syncStateDao().findById(entry.syncStateId)?.let { sync ->
            writeDebug(entry.entryId, "update sync upsert")
            db.syncStateDao().upsert(sync.copy(syncStatus = status, updatedAt = now))
        }
        writeDebug(entry.entryId, "update status done=$status")
    }

    private suspend fun markFailed(entryId: String, code: String, message: String) {
        val entry = db.diaryEntryDao().findById(entryId) ?: return
        val now = toIso8601Utc(nowUtcMillis())
        db.diaryEntryDao().upsert(entry.copy(processingStatus = "processed_failed", updatedAt = now))
        val sync = db.syncStateDao().findById(entry.syncStateId) ?: run {
            SyncStateEntity(
                syncStateId = if (entry.syncStateId.isBlank()) UUID.randomUUID().toString() else entry.syncStateId,
                entityType = "DiaryEntry",
                entityId = entry.entryId,
                userId = entry.userId,
                syncStatus = "processed_failed",
                retryCount = 0,
                lastErrorCode = null,
                lastErrorMessage = null,
                lastAttemptAt = null,
                nextRetryAt = null,
                createdAt = now,
                updatedAt = now
            )
        }
        db.syncStateDao().upsert(
            sync.copy(
                syncStatus = "processed_failed",
                retryCount = sync.retryCount + 1,
                lastErrorCode = code,
                lastErrorMessage = message,
                lastAttemptAt = now,
                updatedAt = now
            )
        )
    }

    private suspend fun writeDebug(entryId: String, message: String) {
        val entry = db.diaryEntryDao().findById(entryId) ?: return
        val now = toIso8601Utc(nowUtcMillis())
        val sync = db.syncStateDao().findById(entry.syncStateId) ?: run {
            SyncStateEntity(
                syncStateId = if (entry.syncStateId.isBlank()) UUID.randomUUID().toString() else entry.syncStateId,
                entityType = "DiaryEntry",
                entityId = entry.entryId,
                userId = entry.userId,
                syncStatus = entry.processingStatus,
                retryCount = 0,
                lastErrorCode = null,
                lastErrorMessage = null,
                lastAttemptAt = null,
                nextRetryAt = null,
                createdAt = now,
                updatedAt = now
            )
        }
        db.syncStateDao().upsert(
            sync.copy(
                lastErrorMessage = "[debug] $message",
                updatedAt = now
            )
        )
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
