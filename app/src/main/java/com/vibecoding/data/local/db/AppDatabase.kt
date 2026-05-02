package com.vibecoding.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vibecoding.data.local.dao.AudioAssetDao
import com.vibecoding.data.local.dao.DiaryEntryDao
import com.vibecoding.data.local.dao.SyncStateDao
import com.vibecoding.data.local.entity.AudioAssetEntity
import com.vibecoding.data.local.entity.DiaryEntryEntity
import com.vibecoding.data.local.entity.SyncStateEntity

@Database(
    entities = [DiaryEntryEntity::class, AudioAssetEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun audioAssetDao(): AudioAssetDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val DATABASE_NAME: String = "diary_mvp.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
