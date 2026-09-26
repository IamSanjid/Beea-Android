package com.hoqit.beea.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hoqit.beea.ui.screens.HomeScreen
import com.hoqit.beea.ui.screens.SettingsScreen

/**
 * Root composable managing screen navigation and transition animations for Beea.
 * Additional screens and navigation routes can be added here easily.
 */
@Composable
fun BeeaApp(
    hasPermissions: Boolean,
    isServiceRunning: Boolean,
    onRequestPermissions: () -> Unit,
    onToggleService: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    // Intercept hardware/gesture back press when not on Home screen
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == Screen.Settings) {
                // Left-to-right transition when visiting Settings
                (slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeIn(animationSpec = tween(350))) togetherWith (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeOut(animationSpec = tween(350)))
            } else {
                // Right-to-left transition when leaving Settings
                (slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeIn(animationSpec = tween(350))) togetherWith (slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeOut(animationSpec = tween(350)))
            }
        },
        label = "screen_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                isServiceRunning = isServiceRunning,
                onOpenSettings = { currentScreen = Screen.Settings }
            )

            Screen.Settings -> SettingsScreen(
                hasPermissions = hasPermissions,
                isServiceRunning = isServiceRunning,
                onRequestPermissions = onRequestPermissions,
                onToggleService = onToggleService,
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
    }
}
