// app/src/main/java/com/mockgps/standalone/service/MockLocationService.kt
package com.mockgps.standalone.service

import android.app.*
import android.content.Intent
import android.location.*
import android.location.provider.ProviderProperties
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mockgps.standalone.R
import kotlinx.coroutines.*

class MockLocationService : Service() {

    companion object {
        const val TAG = "MockGPS"
        const val CHANNEL_ID = "mock_gps"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.mockgps.ACTION_STOP"
    }

    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var walkerJob: Job? = null

    var currentLat = 25.0330
    var currentLon = 121.5654
    var isRunning = false
    var providerReady = false

    private val pushTask = object : Runnable {
        override fun run() {
            if (isRunning) {
                pushLocation(currentLat, currentLon)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotifChannel()
        startForeground(NOTIF_ID, buildNotif(getString(R.string.status_idle)))
        providerReady = initProviders()
        if (!providerReady) updateNotif(getString(R.string.status_no_provider))
    }

    private fun initProviders(): Boolean = try {
        runCatching { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) }
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER,
            false, false, false, false, true, true, true,
            ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

        runCatching { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) }
        locationManager.addTestProvider(
            LocationManager.NETWORK_PROVIDER,
            false, false, false, false, false, false, false,
            ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE
        )
        locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        Log.d(TAG, "Providers initialized")
        true
    } catch (e: SecurityException) {
        Log.e(TAG, "Not selected as mock location app: ${e.message}")
        false
    } catch (e: Exception) {
        Log.e(TAG, "initProviders failed: ${e.message}")
        false
    }

    fun setLocation(lat: Double, lon: Double) {
        if (!providerReady) { providerReady = initProviders() }
        if (!providerReady) { updateNotif(getString(R.string.status_no_provider)); return }
        currentLat = lat; currentLon = lon
        if (!isRunning) { isRunning = true; handler.post(pushTask) }
        updateNotif(getString(R.string.status_static, lat, lon))
    }

    fun startWalker(centerLat: Double, centerLon: Double, radiusM: Double, speedMs: Double) {
        if (!providerReady) { providerReady = initProviders() }
        if (!providerReady) { updateNotif(getString(R.string.status_no_provider)); return }
        // Cancel any existing static push or walker
        isRunning = false
        handler.removeCallbacks(pushTask)
        walkerJob?.cancel()
        currentLat = centerLat; currentLon = centerLon
        isRunning = true
        walkerJob = serviceScope.launch {
            val walker = WalkerState(centerLat, centerLon, radiusM, speedMs)
            while (isActive) {
                val (lat, lon) = walker.step()
                currentLat = lat; currentLon = lon
                pushLocation(lat, lon)
                updateNotif(getString(R.string.status_walker, lat, lon))
                delay(1000)
            }
        }
    }

    fun stopMocking() {
        isRunning = false
        handler.removeCallbacks(pushTask)
        serviceScope.coroutineContext.cancelChildren()
        runCatching { locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false) }
        runCatching { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) }
        runCatching { locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false) }
        runCatching { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER) }
        providerReady = false
        updateNotif(getString(R.string.status_idle))
    }

    fun pushLocation(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtimeNanos()
        val jitter = (Math.random() * 2 - 1).toFloat()
        runCatching {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
                latitude = lat; longitude = lon; altitude = 10.0
                accuracy = 3.0f + jitter; time = now; elapsedRealtimeNanos = elapsed
            })
        }
        runCatching {
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = lat; longitude = lon; altitude = 10.0
                accuracy = 20.0f; time = now; elapsedRealtimeNanos = elapsed
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopMocking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() = this@MockLocationService
    }

    override fun onDestroy() {
        stopMocking()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotifChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotif(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MockLocationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_action_stop), stopIntent)
            .build()
    }

    fun updateNotif(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))
    }
}
