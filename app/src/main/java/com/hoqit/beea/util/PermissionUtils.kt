package com.hoqit.beea.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    /** Permissions this app needs to run the wake-word service, given the current SDK. */
    fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.USE_FULL_SCREEN_INTENT,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasRecordAudioPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }

    fun hasUseFullScreenIntentPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.USE_FULL_SCREEN_INTENT)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return requiredPermissions().all { hasPermission(context, it) }
    }

    fun missingPermissions(context: Context): Array<String> {
        return requiredPermissions().filter { !hasPermission(context, it) }.toTypedArray()
    }
}
