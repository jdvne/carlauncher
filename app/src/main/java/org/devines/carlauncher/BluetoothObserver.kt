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
 *
 * Uses BluetoothProfile.ServiceListener for the initial state query so we
 * correctly detect devices that were already connected before the receiver
 * was registered (common when the launcher restarts).
 */
class BluetoothObserver(
    private val context: Context,
    private val onDeviceChanged: (name: String?) -> Unit
) {
    private var receiver: BroadcastReceiver? = null

    fun start() {
        // Query current state via profile proxy (async but fast)
        queryConnectedDeviceName()

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

    /**
     * Async — connects to the A2DP profile proxy, reads connected devices,
     * then immediately closes the proxy. Calls onDeviceChanged on the main
     * thread via the proxy callback (guaranteed by the framework).
     */
    private fun queryConnectedDeviceName() {
        if (!hasBluetoothPermission()) return
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter ?: return
        if (!adapter.isEnabled) return

        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val name = try {
                    proxy.connectedDevices.firstOrNull()?.let { safeGetName(it) }
                } catch (e: SecurityException) {
                    null
                }
                onDeviceChanged(name)
                adapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {
                // Nothing to do — broadcast receiver handles live disconnects
            }
        }, BluetoothProfile.A2DP)
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
