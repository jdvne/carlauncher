package org.devines.carlauncher

import android.content.Context

object FavoritesStore {
    private const val PREFS = "launcher_prefs"
    private const val KEY   = "favorites"

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())!!.toSet() // defensive copy

    fun save(context: Context, favorites: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, favorites)
            .apply()
    }
}
