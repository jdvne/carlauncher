package org.devines.carlauncher

import android.content.Context

/** Stores debug/simulation flags used to test UI indicators without real hardware. */
object DebugStore {
    private const val PREFS         = "carlauncher_debug"
    private const val KEY_FAKE_BT   = "fake_bt"
    private const val KEY_FAKE_WIFI = "fake_wifi"

    data class DebugState(val fakeBt: Boolean, val fakeWifi: Boolean)

    fun load(context: Context): DebugState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DebugState(
            fakeBt   = p.getBoolean(KEY_FAKE_BT, false),
            fakeWifi = p.getBoolean(KEY_FAKE_WIFI, false)
        )
    }

    fun save(context: Context, state: DebugState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_FAKE_BT, state.fakeBt)
            .putBoolean(KEY_FAKE_WIFI, state.fakeWifi)
            .apply()
    }
}
