package com.vibecoding.data.local

object DiaryProcessingStatus {
    const val DraftRecording = "draft_recording"
    const val RecordedPendingUpload = "recorded_pending_upload"
    const val Processing = "processing"
    const val ProcessedSucceeded = "processed_succeeded"
    const val ProcessedFailed = "processed_failed"

    fun needsProcessing(status: String): Boolean =
        status == RecordedPendingUpload || status == Processing

    fun canRetry(status: String): Boolean =
        status == ProcessedFailed
}

object SyncStatus {
    const val PendingUpload = "pending_upload"
    const val Uploading = "uploading"
    const val Synced = "synced"
    const val Failed = "failed"
    const val Deleted = "deleted"
}
