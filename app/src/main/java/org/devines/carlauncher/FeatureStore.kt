package org.devines.carlauncher

import android.content.Context

/** Persists user-facing feature toggles. Distinct from DebugStore (which is for testing only). */
object FeatureStore {
    private const val PREFS              = "carlauncher_features"
    private const val KEY_RECENT_APPS    = "show_recent_apps"

    data class Features(
        val showRecentApps: Boolean = true
    )

    fun load(context: Context): Features {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Features(
            showRecentApps = p.getBoolean(KEY_RECENT_APPS, true)
        )
    }

    fun save(context: Context, features: Features) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_RECENT_APPS, features.showRecentApps)
            .apply()
    }
}
