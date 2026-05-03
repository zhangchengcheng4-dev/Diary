package com.vibecoding.data.repository

import com.vibecoding.data.network.AudioJobResult
import com.vibecoding.data.network.UploadAudioRequest
import com.vibecoding.data.network.UploadAudioResponse

interface AudioRepository {
    suspend fun uploadAudio(request: UploadAudioRequest): Result<UploadAudioResponse>
    suspend fun getAudioJob(jobId: String): Result<AudioJobResult>
}
