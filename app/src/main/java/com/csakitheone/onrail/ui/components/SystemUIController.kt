package com.csakitheone.onrail.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat

@Composable
fun SystemUIController(
    isStatusBarIconsDark: Boolean = true,
) {
    val activity = LocalActivity.current

    SideEffect {
        if (activity == null) return@SideEffect

        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        insetsController.isAppearanceLightStatusBars = isStatusBarIconsDark
    }
}