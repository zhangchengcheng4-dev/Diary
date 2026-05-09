package com.vibecoding.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vibecoding.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntryEntity)

    @Update
    suspend fun update(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND deletedAt IS NULL ORDER BY entryOccurredAt DESC LIMIT :limit OFFSET :offset")
    fun observeActiveByUser(userId: String, limit: Int = 20, offset: Int = 0): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun findById(entryId: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE entryId = :entryId LIMIT 1")
    fun observeById(entryId: String): Flow<DiaryEntryEntity?>

    @Query("SELECT * FROM diary_entries WHERE processingStatus = :processingStatus AND deletedAt IS NULL")
    suspend fun findByProcessingStatus(processingStatus: String): List<DiaryEntryEntity>

    @Query("UPDATE diary_entries SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE entryId = :entryId")
    suspend fun softDelete(entryId: String, deletedAt: String, updatedAt: String)

    @Query("DELETE FROM diary_entries WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)
}
