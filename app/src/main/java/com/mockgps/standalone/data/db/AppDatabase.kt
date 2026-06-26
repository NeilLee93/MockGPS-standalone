package com.mockgps.standalone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity

@Database(entities = [FavoriteEntity::class, RecentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun instance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mockgps.db")
                .build().also { INSTANCE = it }
        }
    }
}
