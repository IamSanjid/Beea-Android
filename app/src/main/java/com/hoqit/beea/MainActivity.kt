package com.hoqit.beea

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hoqit.beea.service.WakeWordService
import com.hoqit.beea.state.AssistantEvent
import com.hoqit.beea.state.AssistantState
import com.hoqit.beea.ui.navigation.BeeaApp
import com.hoqit.beea.ui.theme.BeeaTheme
import com.hoqit.beea.util.PermissionUtils
import com.hoqit.beea.util.ServiceBinderConnection
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private var hasPermissions by mutableStateOf(false)

    private val wakeWordConnection = ServiceBinderConnection<WakeWordService>(
        context = this,
        serviceClass = WakeWordService::class.java,
        onConnected = { it.pauseListening() } // Activity is foreground, pause passive listening
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.all { it.value }
        if (hasPermissions) startWakeWordService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        hasPermissions = PermissionUtils.hasAllRequiredPermissions(this)
        if (hasPermissions) startWakeWordService()

        handleWakeDetectionExtras(intent)

        setContent {
            val wakeWordService by wakeWordConnection.service.collectAsState()

            BeeaTheme {
                val isListening by AssistantState.isListening.collectAsState()

                LaunchedEffect(Unit) {
                    AssistantState.events.collectLatest { event ->
                        when (event) {
                            is AssistantEvent.WakeWordDetected -> {
                                // TODO: kick off TTS / assistant flow using event.modelName
                            }
                            is AssistantEvent.TtsStarted -> { /* TODO */ }
                            AssistantEvent.TtsFinished -> { /* TODO */ }
                        }
                    }
                }

                BeeaApp(
                    hasPermissions = hasPermissions,
                    isServiceRunning = isListening || wakeWordService != null,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionUtils.missingPermissions(this))
                    },
                    onToggleService = {
                        if (wakeWordService != null) stopWakeWordService() else startWakeWordService()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeDetectionExtras(intent)
    }

    override fun onStart() {
        super.onStart()
        if (hasPermissions) wakeWordConnection.bind()
    }

    override fun onStop() {
        super.onStop()
        wakeWordConnection.service.value?.resumeListening()
        wakeWordConnection.unbind()
    }

    override fun onResume() {
        super.onResume()
        val updatedPermissions = PermissionUtils.hasAllRequiredPermissions(this)
        if (updatedPermissions != hasPermissions) {
            hasPermissions = updatedPermissions
            if (hasPermissions) startWakeWordService()
        }
    }

    private fun handleWakeDetectionExtras(intent: Intent) {
        if (intent.getBooleanExtra("from_wake_detection", false)) {
            val modelName = intent.getStringExtra("detected_model_name")
            wakeWordConnection.service.value?.clearDetectionAlert()
            // TODO: trigger TTS / assistant flow directly here too, if needed on cold start
        }
    }

    private fun startWakeWordService() {
        ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
        wakeWordConnection.bind()
    }

    private fun stopWakeWordService() {
        wakeWordConnection.unbind()
        stopService(Intent(this, WakeWordService::class.java))
    }
}