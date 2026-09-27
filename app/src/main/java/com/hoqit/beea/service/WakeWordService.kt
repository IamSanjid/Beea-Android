package com.hoqit.beea.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hoqit.beea.MainActivity
import com.hoqit.beea.R
import com.hoqit.beea.model.ModelRepository
import com.hoqit.beea.state.AssistantEvent
import com.hoqit.beea.state.AssistantState
import com.hoqit.beea.util.ServiceBinder
import com.hoqit.beea.util.getAppInfo

import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import kotlinx.coroutines.launch

class WakeWordService : LifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeWordEngine: WakeWordEngine

    inner class LocalBinder : Binder(), ServiceBinder<WakeWordService> {
        override fun getService(): WakeWordService = this@WakeWordService
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        wakeWordEngine = WakeWordEngine(
            context = this,
            models = ModelRepository.getModels(),
            detectionMode = DetectionMode.SINGLE_BEST
        )
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification(listening = true))
        startWakeWordEngine()
        return START_STICKY
    }

    fun pauseListening() {
        wakeWordEngine.stop()
        AssistantState.setListening(false)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(listening = false))
    }

    fun resumeListening() {
        wakeWordEngine.start()
        AssistantState.setListening(true)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(listening = true))
    }

    fun clearDetectionAlert() {
        // wait 1 second to clear the notification out on the main loop
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(DETECTION_NOTIFICATION_ID)
        }, 1000L)
    }

    private fun onWakeWordDetected(modelName: String) {
        notificationManager.cancel(DETECTION_NOTIFICATION_ID)
        notificationManager.notify(
            DETECTION_NOTIFICATION_ID,
            buildDetectionAlert(createUIIntent(modelName))
        )
        lifecycleScope.launch {
            AssistantState.emit(AssistantEvent.WakeWordDetected(modelName))
        }
    }

    override fun onDestroy() {
        wakeWordEngine.stop()
        AssistantState.setListening(false)
        super.onDestroy()
    }

    private fun createUIIntent(modelName: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("from_wake_detection", true)
            putExtra("detected_model_name", modelName)
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildNotification(listening: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_ONGOING)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (listening) "Listening for wake word" else "Paused")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun buildDetectionAlert(wakeIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Wake word detected!")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(wakeIntent, true)
            .setContentIntent(wakeIntent)
            .setAutoCancel(true)
            .build()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoing = NotificationChannel(
                CHANNEL_ID_ONGOING, "Wake word listening", NotificationManager.IMPORTANCE_LOW
            )
            val alert = NotificationChannel(
                CHANNEL_ID_ALERT, "Wake word detected", NotificationManager.IMPORTANCE_HIGH
            ).apply { lockscreenVisibility = Notification.VISIBILITY_PUBLIC }
            notificationManager.createNotificationChannel(ongoing)
            notificationManager.createNotificationChannel(alert)
        }
    }

    private fun startWakeWordEngine() {
        wakeWordEngine.start()
        AssistantState.setListening(true)

        val appName = getAppInfo().name

        lifecycleScope.launch {
            wakeWordEngine.detections.collect { detection ->
                Log.d("${appName}-WakeWordService", "Detected: ${detection.model.name} (score: ${detection.score})")
                onWakeWordDetected(detection.model.name)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID_ONGOING = "wake_word_channel"
        private const val CHANNEL_ID_ALERT = "wake_word_alert_channel"
        private const val NOTIFICATION_ID = 1
        private const val DETECTION_NOTIFICATION_ID = 2
    }
}