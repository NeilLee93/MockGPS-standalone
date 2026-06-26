package com.mockgps.standalone.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.repository.LocationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: LocationRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = LocationRepository(db)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun addAndRetrieveFavorite() = runTest {
        repo.addFavorite("台北車站", 25.0478, 121.5170)
        val list = repo.favorites.first()
        assertEquals(1, list.size)
        assertEquals("台北車站", list[0].name)
    }

    @Test
    fun deleteFavorite() = runTest {
        repo.addFavorite("台北", 25.0, 121.0)
        val id = repo.favorites.first()[0].id
        repo.deleteFavorite(id)
        assertTrue(repo.favorites.first().isEmpty())
    }

    @Test
    fun recentsTrimsToTen() = runTest {
        repeat(15) { repo.addRecent("地點$it", 25.0 + it * 0.001, 121.0) }
        val list = repo.recents.first()
        assertEquals(10, list.size)
    }
}
