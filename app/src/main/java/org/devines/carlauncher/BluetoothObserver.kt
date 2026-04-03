package org.devines.carlauncher

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build

/**
 * Observes Bluetooth connection state and reports the connected device name.
 * Emits null when no device is connected.
 */
class BluetoothObserver(
    private val context: Context,
    private val onDeviceChanged: (name: String?) -> Unit
) {
    private var receiver: BroadcastReceiver? = null

    fun start() {
        // Emit current state immediately
        onDeviceChanged(queryConnectedDeviceName())

        // Listen for future connect/disconnect events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        onDeviceChanged(safeGetName(device))
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDeviceChanged(null)
                }
            }
        }
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }

    private fun queryConnectedDeviceName(): String? {
        if (!hasBluetoothPermission()) return null
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter ?: return null
        if (!adapter.isEnabled) return null
        return try {
            // Check if any bonded device is connected via headset or A2DP (audio profiles)
            adapter.bondedDevices
                ?.firstOrNull { device ->
                    adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                        BluetoothProfile.STATE_CONNECTED ||
                    adapter.getProfileConnectionState(BluetoothProfile.A2DP) ==
                        BluetoothProfile.STATE_CONNECTED
                }
                ?.let { safeGetName(it) }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun safeGetName(device: BluetoothDevice?): String? {
        if (device == null) return null
        return try { device.name } catch (e: SecurityException) { null }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true // API 26–30: BLUETOOTH is a normal permission, granted at install
        }
    }
}
