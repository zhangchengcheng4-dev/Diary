package com.vibecoding.data.repository

import com.vibecoding.data.network.CreateDiaryRequest
import com.vibecoding.data.network.DiaryDetailDto
import com.vibecoding.data.network.DiarySummaryDto
import com.vibecoding.data.network.UpdateDiaryRequest

interface DiaryRepository {
    suspend fun getDiaries(page: Int, pageSize: Int = 20): Result<List<DiarySummaryDto>>
    suspend fun getDiary(entryId: String): Result<DiaryDetailDto>
    suspend fun createDiary(request: CreateDiaryRequest): Result<DiaryDetailDto>
    suspend fun updateDiary(entryId: String, request: UpdateDiaryRequest): Result<DiaryDetailDto>
    suspend fun deleteDiary(entryId: String): Result<Unit>
}
