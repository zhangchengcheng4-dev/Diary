package com.vibecoding.data.network

data class UploadAudioRequest(
    val userId: String,
    val fileName: String,
    val contentType: String,
    val durationSeconds: Int
)

data class UploadAudioResponse(
    val jobId: String,
    val audioAssetId: String
)

data class AudioJobResult(
    val jobId: String,
    val status: String,
    val transcript: String?,
    val category: String?,
    val tags: List<String>?,
    val polishedArticle: String?
)

interface AudioApi {
    suspend fun uploadAudio(request: UploadAudioRequest): ApiEnvelope<UploadAudioResponse>
    suspend fun getAudioJob(jobId: String): ApiEnvelope<AudioJobResult>
}
