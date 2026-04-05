package org.devines.carlauncher

import android.content.Context

/**
 * Persists an ordered list of recently launched package names.
 * Most recent is first. Capped at MAX_RECENT entries.
 */
object RecentAppsStore {
    private const val PREFS    = "carlauncher_recent"
    private const val KEY_LIST = "recent_packages"
    private const val MAX_RECENT = 8

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LIST, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(",").filter { it.isNotBlank() }
    }

    /** Call this whenever an app is launched — moves it to the front of the list. */
    fun recordLaunch(context: Context, packageName: String) {
        val updated = load(context).toMutableList().also {
            it.remove(packageName)  // remove existing entry so no duplicates
            it.add(0, packageName)  // push to front
        }.take(MAX_RECENT)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIST, updated.joinToString(",")).apply()
    }
}
