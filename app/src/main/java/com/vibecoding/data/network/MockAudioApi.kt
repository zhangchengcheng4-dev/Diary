package com.vibecoding.data.network

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MockAudioApi : AudioApi {
    private val pollCountByJobId = ConcurrentHashMap<String, Int>()

    override suspend fun uploadAudio(request: UploadAudioRequest): ApiEnvelope<UploadAudioResponse> {
        val jobId = "mock-job-${UUID.randomUUID()}"
        val audioAssetId = "mock-asset-${UUID.randomUUID()}"
        pollCountByJobId[jobId] = 0
        return ApiEnvelope(
            requestId = "mock-req-upload-${UUID.randomUUID()}",
            serverTime = "2026-05-04T00:00:00Z",
            data = UploadAudioResponse(
                jobId = jobId,
                audioAssetId = audioAssetId
            ),
            error = null
        )
    }

    override suspend fun getAudioJob(jobId: String): ApiEnvelope<AudioJobResult> {
        val count = (pollCountByJobId[jobId] ?: 0) + 1
        pollCountByJobId[jobId] = count
        val status = if (count >= 3) "succeeded" else "processing"
        val transcript = "这是一次模拟语音转写结果。"
        val result = AudioJobResult(
            jobId = jobId,
            status = status,
            transcript = if (status == "succeeded") transcript else null,
            category = if (status == "succeeded") "mood" else null, // Temporary AI placeholder.
            tags = if (status == "succeeded") emptyList() else null, // Temporary AI placeholder.
            polishedArticle = if (status == "succeeded") transcript else null // Temporary AI placeholder.
        )
        return ApiEnvelope(
            requestId = "mock-req-job-${UUID.randomUUID()}",
            serverTime = "2026-05-04T00:00:00Z",
            data = result,
            error = null
        )
    }
}
