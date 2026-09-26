package com.hoqit.beea.util

import android.content.Context
import com.hoqit.beea.BuildConfig
import com.hoqit.beea.R

data class AppInfo(
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val description: String
)

fun Context.getAppInfo(): AppInfo = AppInfo(
    name = getString(R.string.app_name),
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE.toLong(),
    description = getString(R.string.app_description)
)
