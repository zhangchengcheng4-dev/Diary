package com.vibecoding.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibecoding.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT * FROM sync_states WHERE syncStateId = :syncStateId LIMIT 1")
    suspend fun findById(syncStateId: String): SyncStateEntity?

    @Query("SELECT * FROM sync_states WHERE userId = :userId AND syncStatus != 'synced'")
    fun observePendingByUser(userId: String): Flow<List<SyncStateEntity>>

    @Query("DELETE FROM sync_states WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)
}
