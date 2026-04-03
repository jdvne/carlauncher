package org.devines.carlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

private const val UPDATE_FILENAME = "carlauncher.apk"

/**
 * Silently scans USB storage for carlauncher.apk.
 * Returns the File if found, null otherwise.
 */
fun Activity.findAvailableUpdate(): File? = findUpdateApk()

/**
 * Shows an install prompt for a found APK file.
 * Call this when the user taps the update button.
 */
fun Activity.promptInstallUpdate(apk: File) {
    AlertDialog.Builder(this)
        .setTitle("Install update")
        .setMessage("Found carlauncher.apk on USB. Install now?")
        .setPositiveButton("Install") { _, _ -> installApk(apk) }
        .setNegativeButton("Cancel", null)
        .show()
}

/** Searches common external/USB mount points for the update APK. */
private fun findUpdateApk(): File? {
    val roots = mutableListOf<File>()
    roots.add(Environment.getExternalStorageDirectory())
    listOf("/storage", "/mnt/media_rw", "/mnt/usb").forEach { base ->
        File(base).listFiles()?.forEach { roots.add(it) }
    }
    for (root in roots) {
        val candidate = File(root, UPDATE_FILENAME)
        if (candidate.exists() && candidate.canRead()) return candidate
        root.listFiles()?.forEach { sub ->
            val deep = File(sub, UPDATE_FILENAME)
            if (deep.exists() && deep.canRead()) return deep
        }
    }
    return null
}

private fun Activity.installApk(apk: File) {
    val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
