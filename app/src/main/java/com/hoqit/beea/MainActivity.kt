package com.hoqit.beea

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hoqit.beea.service.WakeWordService
import com.hoqit.beea.ui.navigation.BeeaApp
import com.hoqit.beea.ui.theme.BeeaTheme
import com.hoqit.beea.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private var hasPermissions by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.all { it.value }
        if (hasPermissions && !isServiceRunning) {
            startWakeWordService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasPermissions = PermissionUtils.hasAllRequiredPermissions(this)

        if (hasPermissions) {
            startWakeWordService()
        }

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            BeeaTheme {
                BeeaApp(
                    hasPermissions = hasPermissions,
                    isServiceRunning = isServiceRunning,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionUtils.missingPermissions(this))
                    },
                    onToggleService = {
                        if (isServiceRunning) stopWakeWordService() else startWakeWordService()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val updatedPermissions = PermissionUtils.hasAllRequiredPermissions(this)
        if (updatedPermissions != hasPermissions) {
            hasPermissions = updatedPermissions
            if (hasPermissions && !isServiceRunning) {
                startWakeWordService()
            }
        }
    }

    private fun startWakeWordService() {
        val intent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
    }

    private fun stopWakeWordService() {
        stopService(Intent(this, WakeWordService::class.java))
        isServiceRunning = false
    }
}
