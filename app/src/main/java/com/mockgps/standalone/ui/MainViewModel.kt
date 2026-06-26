// app/src/main/java/com/mockgps/standalone/ui/MainViewModel.kt
package com.mockgps.standalone.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity
import com.mockgps.standalone.data.db.AppDatabase
import com.mockgps.standalone.data.repository.LocationRepository
import com.mockgps.standalone.service.MockLocationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class MockMode { STATIC, WALKER }

data class SearchResult(val name: String, val displayName: String, val lat: Double, val lon: Double)

data class UiState(
    val lat: Double = 25.0330,
    val lon: Double = 121.5654,
    val mode: MockMode = MockMode.STATIC,
    val walkerRadius: Float = 300f,
    val walkerSpeed: Float = 3f,
    val isRunning: Boolean = false,
    val providerReady: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LocationRepository(AppDatabase.instance(app))

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<FavoriteEntity>> = repo.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<RecentEntity>> = repo.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var service: MockLocationService? = null
    private var searchJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as MockLocationService.LocalBinder).getService()
            _uiState.update { it.copy(isRunning = service?.isRunning ?: false, providerReady = service?.providerReady ?: false) }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _uiState.update { it.copy(isRunning = false, providerReady = false) }
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, MockLocationService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        runCatching { context.unbindService(connection) }
    }

    fun syncFromService() {
        service?.let { svc ->
            _uiState.update { it.copy(isRunning = svc.isRunning, providerReady = svc.providerReady) }
        }
    }

    fun onMapTap(lat: Double, lon: Double) {
        _uiState.update { it.copy(lat = lat, lon = lon) }
    }

    fun onStartStatic() {
        val s = _uiState.value
        val svc = service ?: return   // guard: don't update state if service not bound
        svc.setLocation(s.lat, s.lon)
        _uiState.update { it.copy(isRunning = true, mode = MockMode.STATIC) }
        viewModelScope.launch { repo.addRecent(null, s.lat, s.lon) }
    }

    fun onStartWalker() {
        val s = _uiState.value
        val svc = service ?: return   // guard
        svc.startWalker(s.lat, s.lon, s.walkerRadius.toDouble(), s.walkerSpeed.toDouble())
        _uiState.update { it.copy(isRunning = true, mode = MockMode.WALKER) }
        viewModelScope.launch { repo.addRecent(null, s.lat, s.lon) }
    }

    fun onStop() {
        service?.stopMocking()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) { _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }; return }
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isSearching = true) }
            val results = runCatching { nominatimSearch(query) }.getOrDefault(emptyList())
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun onSearchResultSelected(result: SearchResult) {
        _uiState.update { it.copy(lat = result.lat, lon = result.lon, searchQuery = result.name, searchResults = emptyList()) }
        viewModelScope.launch { repo.addRecent(result.name, result.lat, result.lon) }
    }

    fun onAddFavorite(name: String) {
        val s = _uiState.value
        viewModelScope.launch { repo.addFavorite(name, s.lat, s.lon) }
    }

    fun onDeleteFavorite(id: Long) {
        viewModelScope.launch { repo.deleteFavorite(id) }
    }

    fun onLocationListItemTapped(lat: Double, lon: Double, name: String?) {
        _uiState.update { it.copy(lat = lat, lon = lon) }
    }

    fun onModeChange(mode: MockMode) { _uiState.update { it.copy(mode = mode) } }
    fun onRadiusChanged(meters: Float) { _uiState.update { it.copy(walkerRadius = meters) } }
    fun onSpeedChanged(ms: Float) { _uiState.update { it.copy(walkerSpeed = ms) } }

    private suspend fun nominatimSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&accept-language=zh-TW,zh,en")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "MockGPS-Standalone/1.0")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.errorStream?.close()
                return@withContext emptyList()
            }
            val body = conn.inputStream.bufferedReader().readText()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val display = obj.getString("display_name")
                SearchResult(
                    name = display.substringBefore(",").trim(),
                    displayName = display,
                    lat = obj.getString("lat").toDouble(),
                    lon = obj.getString("lon").toDouble()
                )
            }
        } finally { conn.disconnect() }
    }
}
