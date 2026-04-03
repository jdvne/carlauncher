package org.devines.carlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class AppDrawerActivity : Activity() {

    private var pendingUpdate: File? = null

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
            if (update != null) {
                promptInstallUpdate(update)
            } else {
                Toast.makeText(this, "No carlauncher.apk found on USB", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<LinearLayout>(R.id.btnInfo).setOnClickListener {
            showInfoDialog()
        }

        val appGrid = findViewById<RecyclerView>(R.id.appGrid)
        val columnWidth = resources.getDimensionPixelSize(R.dimen.grid_column_width)
        val spanCount = (resources.displayMetrics.widthPixels / columnWidth).coerceAtLeast(1)
        appGrid.layoutManager = GridLayoutManager(this, spanCount)
        appGrid.adapter = AppGridAdapter(loadInstalledApps()) { launchApp(it) }

        // Check for update silently in background thread so it doesn't block the UI
        Thread {
            val update = findAvailableUpdate()
            runOnUiThread { setUpdateState(update) }
        }.start()
    }

    private fun showInfoDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("carlauncher")
            .setMessage(
                "To update:\n" +
                "1. Build a new APK in Android Studio\n" +
                "2. Rename it to carlauncher.apk\n" +
                "3. Copy it to the root of a USB stick\n" +
                "4. Plug the USB stick into the head unit\n" +
                "5. Open All Apps — the update button will turn green if a newer version is detected\n" +
                "6. Tap the update button to install"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setUpdateState(update: File?) {
        pendingUpdate = update
        val icon  = findViewById<ImageView>(R.id.iconUpdate)
        val label = findViewById<TextView>(R.id.labelUpdate)
        if (update != null) {
            icon.setImageResource(R.drawable.ic_update_available)
            label.text = "Update available"
            label.setTextColor(0xFF4CAF50.toInt()) // green
        } else {
            icon.setImageResource(R.drawable.ic_update)
            label.text = ""
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Log.w("CarLauncher", "No launch intent for ${app.packageName}")
            Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}
