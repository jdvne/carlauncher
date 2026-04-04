package org.devines.carlauncher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val PREFS              = "launcher_prefs"
        private const val KEY_CARLINK        = "pkg_slot_carlink"
        private const val KEY_RADIO          = "pkg_slot_radio"
        private const val KEY_SETTINGS       = "pkg_slot_settings"
        private const val REQUEST_BT_PERM    = 1001
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var bluetoothObserver: BluetoothObserver
    private lateinit var bluetoothLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        bindConfigurableTile(R.id.tileCarLink,  R.id.iconCarLink,  R.id.labelCarLink,  KEY_CARLINK)
        bindConfigurableTile(R.id.tileRadio,    R.id.iconRadio,    R.id.labelRadio,    KEY_RADIO)
        bindConfigurableTile(R.id.tileSettings, R.id.iconSettings, R.id.labelSettings, KEY_SETTINGS)

        findViewById<LinearLayout>(R.id.tileAllApps).setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
            noTransition()
        }

        bluetoothLabel = findViewById(R.id.bluetoothLabel)
        bluetoothObserver = BluetoothObserver(this) { name ->
            runOnUiThread {
                if (name != null) {
                    bluetoothLabel.text = name
                    bluetoothLabel.visibility = View.VISIBLE
                } else {
                    bluetoothLabel.visibility = View.GONE
                }
            }
        }
        startBluetoothObserver()

        // Debug: long-press the clock to toggle a fake BT device (emulator testing only)
        findViewById<android.widget.TextClock>(R.id.clock).setOnLongClickListener {
            if (bluetoothLabel.visibility == View.VISIBLE) {
                bluetoothLabel.visibility = View.GONE
            } else {
                bluetoothLabel.text = "Josh's iPhone"
                bluetoothLabel.visibility = View.VISIBLE
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothObserver.stop()
    }

    // ── Bluetooth ────────────────────────────────────────────────────

    private fun startBluetoothObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED) {
                bluetoothObserver.start()
            } else {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BT_PERM)
            }
        } else {
            bluetoothObserver.start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_BT_PERM &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            bluetoothObserver.start()
        }
    }

    // ── Configurable tile ────────────────────────────────────────────

    private fun bindConfigurableTile(tileId: Int, iconId: Int, labelId: Int, prefKey: String) {
        val tile  = findViewById<LinearLayout>(tileId)
        val icon  = findViewById<ImageView>(iconId)
        val label = findViewById<TextView>(labelId)
        val pkg   = prefs.getString(prefKey, null)

        if (pkg != null) {
            try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(pkg, 0)
                }
                icon.setImageDrawable(packageManager.getApplicationIcon(pkg))
                icon.alpha = 1f
                label.text = packageManager.getApplicationLabel(info)
                label.setTextColor(getColor(R.color.text_primary))
                tile.setOnClickListener { launchPackage(pkg) }
            } catch (e: PackageManager.NameNotFoundException) {
                prefs.edit().remove(prefKey).apply()
                setUnassignedState(tile, icon, label, prefKey)
            }
        } else {
            setUnassignedState(tile, icon, label, prefKey)
        }

        tile.setOnLongClickListener {
            showAppPicker(prefKey) { bindConfigurableTile(tileId, iconId, labelId, prefKey) }
            true
        }
    }

    private fun setUnassignedState(tile: LinearLayout, icon: ImageView, label: TextView, prefKey: String) {
        icon.setImageDrawable(getDrawable(R.drawable.ic_add))
        icon.alpha = 1f
        label.text = "Tap to set"
        label.setTextColor(getColor(R.color.text_secondary))
        tile.setOnClickListener {
            showAppPicker(prefKey) {
                val (tileId, iconId, labelId) = when (prefKey) {
                    KEY_CARLINK  -> Triple(R.id.tileCarLink,  R.id.iconCarLink,  R.id.labelCarLink)
                    KEY_RADIO    -> Triple(R.id.tileRadio,    R.id.iconRadio,    R.id.labelRadio)
                    else         -> Triple(R.id.tileSettings, R.id.iconSettings, R.id.labelSettings)
                }
                bindConfigurableTile(tileId, iconId, labelId, prefKey)
            }
        }
    }

    // ── App picker ───────────────────────────────────────────────────

    private fun showAppPicker(prefKey: String, onPicked: () -> Unit) {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = queryLaunchableApps(pm, intent)
            .filter { it.activityInfo.packageName != packageName }
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }

        AlertDialog.Builder(this)
            .setTitle("Choose app")
            .setItems(apps.map { it.label }.toTypedArray()) { _, i ->
                prefs.edit().putString(prefKey, apps[i].packageName).apply()
                onPicked()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun launchPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(this, "App not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {} // launchers must not exit
}
