package com.hoqit.beea.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hoqit.beea.MainActivity
import com.hoqit.beea.model.ModelRepository
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import kotlinx.coroutines.launch

class WakeWordService : LifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeWordEngine: WakeWordEngine
    private lateinit var powerManager: PowerManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
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

    private fun onWakeWordDetected() {
        //wakeScreenBriefly()
        notificationManager.notify(
            DETECTION_NOTIFICATION_ID,
            buildDetectionAlert(createUIIntent())
        )
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(DETECTION_NOTIFICATION_ID)
        }, 1000L)
        // TODO: start listening for the follow-up command / STT session here
    }

    override fun onDestroy() {
        wakeWordEngine.stop()
        super.onDestroy()
    }

    /** Ongoing "listening" notification — low importance, stays silent, lives in status bar only. */
    private fun buildNotification(listening: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_ONGOING)
            .setContentTitle("Beea")
            .setContentText(if (listening) "Listening for wake word" else "Idle")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /** One-shot high-priority alert — shows on lockscreen, pops as heads-up. */
    @SuppressLint("FullScreenIntentPolicy")
    private fun buildDetectionAlert(wakeIntent: PendingIntent? = null): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setContentTitle("Beea")
            .setContentText("Wake word detected!")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(wakeIntent, true)
            .setAutoCancel(true)
            .build()
    }

    private fun createUIIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return pendingIntent
    }

    private fun wakeScreenBriefly() {
//        val wakeLock = powerManager.newWakeLock(
//            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
//            "Beea:WakeWordDetection"
//        )
//        wakeLock.acquire(3000L) // auto-releases after 3s, avoids leaking a held lock


        // Crucial for Android 14+ background launch compatibility
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
//            val options = ActivityOptions.makeBasic().apply {
//                setPendingIntentBackgroundActivityStartMode(
//                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
//                )
//            }
//            startActivity(intent, options.toBundle())
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            val options = ActivityOptions.makeBasic().apply {
//                setPendingIntentBackgroundActivityStartMode(
//                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
//                )
//            }
//            startActivity(intent, options.toBundle())
//        }  else {
//            startActivity(intent)
//        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoing = NotificationChannel(
                CHANNEL_ID_ONGOING,
                "Wake word listening",
                NotificationManager.IMPORTANCE_LOW
            )
            val alert = NotificationChannel(
                CHANNEL_ID_ALERT,
                "Wake word detected",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(ongoing)
            notificationManager.createNotificationChannel(alert)
        }
    }

    private fun startWakeWordEngine() {
        wakeWordEngine.start()

        lifecycleScope.launch {
            wakeWordEngine.detections.collect { detection ->
                Log.d("Beea-WakeWord-Service", "Detected: ${detection.model.name} (score: ${detection.score})")
                onWakeWordDetected()
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