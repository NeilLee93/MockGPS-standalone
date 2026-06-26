package com.mockgps.standalone.data.repository

import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity

class LocationRepository(db: AppDatabase) {
    private val favoriteDao = db.favoriteDao()
    private val recentDao = db.recentDao()

    val favorites = favoriteDao.getAll()
    val recents = recentDao.getLatest()

    suspend fun addFavorite(name: String, lat: Double, lon: Double) {
        favoriteDao.insert(FavoriteEntity(name = name, lat = lat, lon = lon))
    }

    suspend fun deleteFavorite(id: Long) {
        favoriteDao.deleteById(id)
    }

    suspend fun addRecent(name: String?, lat: Double, lon: Double) {
        recentDao.insert(RecentEntity(name = name, lat = lat, lon = lon))
        recentDao.trimToTen()
    }
}
