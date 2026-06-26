package com.mockgps.standalone.data.db

import androidx.room.*
import com.mockgps.standalone.data.model.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY usedAt DESC LIMIT 10")
    fun getLatest(): Flow<List<RecentEntity>>

    @Insert
    suspend fun insert(entity: RecentEntity)

    @Query("DELETE FROM recents WHERE id NOT IN (SELECT id FROM recents ORDER BY usedAt DESC LIMIT 10)")
    suspend fun trimToTen()
}
