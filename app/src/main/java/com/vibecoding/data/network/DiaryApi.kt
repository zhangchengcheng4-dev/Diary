package com.vibecoding.data.network

data class DiarySummaryDto(
    val entryId: String,
    val title: String,
    val preview: String,
    val category: String,
    val tags: List<String>,
    val entryOccurredAt: String
)

data class DiaryDetailDto(
    val entryId: String,
    val title: String,
    val transcript: String,
    val polishedArticle: String,
    val category: String,
    val tags: List<String>,
    val entryOccurredAt: String,
    val updatedAt: String
)

data class CreateDiaryRequest(
    val title: String,
    val transcript: String,
    val polishedArticle: String,
    val category: String,
    val tags: List<String>,
    val entryOccurredAt: String
)

data class UpdateDiaryRequest(
    val title: String,
    val polishedArticle: String,
    val category: String,
    val tags: List<String>,
    val entryOccurredAt: String
)

interface DiaryApi {
    suspend fun getDiaries(page: Int, pageSize: Int = 20): ApiEnvelope<PageResponse<DiarySummaryDto>>
    suspend fun getDiary(entryId: String): ApiEnvelope<DiaryDetailDto>
    suspend fun createDiary(request: CreateDiaryRequest): ApiEnvelope<DiaryDetailDto>
    suspend fun updateDiary(entryId: String, request: UpdateDiaryRequest): ApiEnvelope<DiaryDetailDto>
    suspend fun deleteDiary(entryId: String): ApiEnvelope<Unit>
}
