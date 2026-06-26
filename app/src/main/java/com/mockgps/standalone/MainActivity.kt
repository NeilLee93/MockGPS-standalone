// app/src/main/java/com/mockgps/standalone/MainActivity.kt
package com.mockgps.standalone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mockgps.standalone.ui.MainViewModel
import com.mockgps.standalone.ui.screen.MainScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("mockgps_prefs", MODE_PRIVATE) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val shouldShowBatteryPrompt = !prefs.getBoolean("battery_prompt_shown", false)
            && !isBatteryOptimizationIgnored()

        setContent {
            var showBatteryDialog by remember { mutableStateOf(shouldShowBatteryPrompt) }

            if (showBatteryDialog) {
                AlertDialog(
                    onDismissRequest = {
                        prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                        showBatteryDialog = false
                    },
                    title = { Text(stringResource(R.string.battery_dialog_title)) },
                    text = { Text(stringResource(R.string.battery_dialog_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                            showBatteryDialog = false
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:$packageName")))
                        }) { Text(stringResource(R.string.battery_dialog_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                            showBatteryDialog = false
                        }) { Text(stringResource(R.string.battery_dialog_skip)) }
                    }
                )
            }

            MainScreen(vm = vm)
        }
    }

    override fun onStart() {
        super.onStart()
        vm.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        vm.unbindService(this)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}
