package com.vibecoding.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibecoding.data.local.entity.AudioAssetEntity

@Dao
interface AudioAssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: AudioAssetEntity)

    @Query("SELECT * FROM audio_assets WHERE audioAssetId = :audioAssetId LIMIT 1")
    suspend fun findById(audioAssetId: String): AudioAssetEntity?

    @Query("SELECT * FROM audio_assets WHERE entryId = :entryId LIMIT 1")
    suspend fun findByEntryId(entryId: String): AudioAssetEntity?

    @Query("SELECT * FROM audio_assets WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun findAllByEntryId(entryId: String): List<AudioAssetEntity>

    @Query("SELECT * FROM audio_assets")
    suspend fun findAll(): List<AudioAssetEntity>

    @Query("DELETE FROM audio_assets WHERE localPath = :localPath")
    suspend fun deleteByLocalPath(localPath: String)

    @Query("DELETE FROM audio_assets WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)
}
