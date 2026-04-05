package org.devines.carlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class AppDrawerActivity : Activity() {

    private var pendingUpdate: File? = null
    private lateinit var gridAdapter: AppGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        setContentView(R.layout.activity_app_drawer)

        findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            finish()
            noTransition()
        }

        findViewById<LinearLayout>(R.id.btnUpdate).setOnClickListener {
            val update = pendingUpdate
            if (update != null) promptInstallUpdate(update)
            else Toast.makeText(this, "No carlauncher.apk found on USB", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.btnInfo).setOnClickListener {
            showInfoDialog()
        }

        val appGrid   = findViewById<RecyclerView>(R.id.appGrid)
        val columnWidth = resources.getDimensionPixelSize(R.dimen.grid_column_width)
        val spanCount   = (resources.displayMetrics.widthPixels / columnWidth).coerceAtLeast(1)
        val glm         = GridLayoutManager(this, spanCount)

        val features = FeatureStore.load(this)
        gridAdapter = AppGridAdapter(
            allApps           = loadInstalledApps(),
            favorites         = FavoritesStore.load(this),
            recentPackages    = if (features.showRecentApps) RecentAppsStore.load(this) else emptyList(),
            onAppClick        = { launchApp(it) },
            onFavoriteToggled = { favs -> FavoritesStore.save(this, favs) }
        )

        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =
                gridAdapter.spanSizeAt(position, glm.spanCount)
        }
        glm.spanSizeLookup.isSpanIndexCacheEnabled = true

        appGrid.layoutManager = glm
        appGrid.adapter = gridAdapter

        Thread {
            val update = findAvailableUpdate()
            runOnUiThread { setUpdateState(update) }
        }.start()
    }

    private fun showInfoDialog() {
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()
        val debug    = DebugStore.load(this)
        val features = FeatureStore.load(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (pad * 0.5f).toInt(), pad, 0)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun sectionHeader(title: String) = TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(0xFF777777.toInt())
            setPadding(0, (16 * dp).toInt(), 0, (4 * dp).toInt())
        }

        // ── Info text ────────────────────────────────────────────────
        container.addView(TextView(this).apply {
            text =
                "To update:\n" +
                "1. Build a new APK in Android Studio\n" +
                "2. Copy carlauncher.apk to the root of a USB stick\n" +
                "3. Plug the USB stick into the head unit\n" +
                "4. Open All Apps — the update button turns green when an APK is found\n" +
                "5. Tap the update button to install\n\n" +
                "To favorite an app:\n" +
                "Long-press any app to add it to the Favorites section at the top."
            textSize = 14f
        })

        // ── Features ─────────────────────────────────────────────────
        container.addView(sectionHeader("Features"))

        val recentAppsBox = CheckBox(this).apply {
            text = "Show Recent Apps section in drawer"
            isChecked = features.showRecentApps
        }
        container.addView(recentAppsBox)

        // ── Debug (emulator testing) ──────────────────────────────────
        container.addView(sectionHeader("Simulate indicators (emulator testing)"))

        val fakeBtBox = CheckBox(this).apply {
            text = "Bluetooth connected"
            isChecked = debug.fakeBt
        }
        container.addView(fakeBtBox)

        val fakeWifiBox = CheckBox(this).apply {
            text = "WiFi connected"
            isChecked = debug.fakeWifi
        }
        container.addView(fakeWifiBox)

        android.app.AlertDialog.Builder(this)
            .setTitle("carlauncher")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                FeatureStore.save(this, FeatureStore.Features(
                    showRecentApps = recentAppsBox.isChecked
                ))
                DebugStore.save(this, DebugStore.DebugState(
                    fakeBt   = fakeBtBox.isChecked,
                    fakeWifi = fakeWifiBox.isChecked
                ))
            }
            .show()
    }

    private fun setUpdateState(update: File?) {
        pendingUpdate = update
        val icon  = findViewById<ImageView>(R.id.iconUpdate)
        val label = findViewById<TextView>(R.id.labelUpdate)
        if (update != null) {
            icon.setImageResource(R.drawable.ic_update_available)
            label.text = "Update available"
            label.setTextColor(0xFF4CAF50.toInt())
        } else {
            icon.setImageResource(R.drawable.ic_update)
            label.text = ""
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        noTransition()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return queryLaunchableApps(pm, intent)
            .filter { it.activityInfo.packageName != packageName }
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            RecentAppsStore.recordLaunch(this, app.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Log.w("CarLauncher", "No launch intent for ${app.packageName}")
            Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}
