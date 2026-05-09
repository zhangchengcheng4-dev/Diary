package com.vibecoding.recording

data class RecordingSegment(
    val audioAssetId: String,
    val localPath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val mimeType: String
)

data class RecordingDraftResult(
    val entryId: String,
    val totalDurationMs: Long,
    val segments: List<RecordingSegment>
)
