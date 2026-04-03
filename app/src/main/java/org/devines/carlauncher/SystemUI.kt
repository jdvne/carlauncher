package org.devines.carlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Returns all launchable apps using the correct API for the running OS version. */
fun queryLaunchableApps(pm: PackageManager, intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
}

/** Suppresses the activity enter/exit animation — prevents nav bar flash during transitions. */
fun Activity.noTransition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

/** Hides the navigation bar (and status bar) in sticky immersive mode. */
fun Activity.hideSystemUI() {
    // Transparent bars so any brief re-appearance blends with the background
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
