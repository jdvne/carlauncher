package org.devines.carlauncher

import android.graphics.drawable.Drawable

/** Minimal data class representing a launchable app. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
