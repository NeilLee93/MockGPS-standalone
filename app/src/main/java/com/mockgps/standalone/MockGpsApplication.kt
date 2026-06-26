package com.mockgps.standalone

import android.app.Application
import org.osmdroid.config.Configuration

class MockGpsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@MockGpsApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "MockGPS-Standalone/1.0"
        }
    }
}
