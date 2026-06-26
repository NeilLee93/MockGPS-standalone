package com.mockgps.standalone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val lat: Double,
    val lon: Double,
    val usedAt: Long = System.currentTimeMillis()
)
