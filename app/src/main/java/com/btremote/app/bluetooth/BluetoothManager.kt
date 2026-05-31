package com.btremote.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val raw: BluetoothDevice,
    val bonded: Boolean = false
)

@Singleton
class BluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val androidManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val adapter: BluetoothAdapter? = androidManager?.adapter

    private val _scanResults = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val scanResults: StateFlow<List<DiscoveredDevice>> = _scanResults.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private var scanner: BluetoothLeScanner? = null
    private val seen = mutableMapOf<String, DiscoveredDevice>()
    private var classicReceiverRegistered = false

    private val emitScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
    )
    private var emitJob: kotlinx.coroutines.Job? = null
    @Volatile private var pendingEmit = false
    private val emitIntervalMs = 200L

    private val classicReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                    val d = device ?: return
                    if (!hasConnectPermission()) return
                    val name = runCatching { d.name }.getOrNull() ?: "(unknown)"
                    val bonded = runCatching { d.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
                    synchronized(seen) {
                        seen[d.address] = DiscoveredDevice(name, d.address, rssi, d, bonded)
                    }
                    scheduleEmit()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Classic ended; LE may still be running. Keep _scanning true until stopScan or both done.
                }
            }
        }
    }

    fun isBluetoothSupported(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<DiscoveredDevice> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices.orEmpty().map {
            DiscoveredDevice(
                name = runCatching { it.name }.getOrNull() ?: "(unknown)",
                address = it.address,
                rssi = 0,
                raw = it,
                bonded = true
            )
        }
    }

    fun deviceFromAddress(address: String): BluetoothDevice? = try {
        adapter?.getRemoteDevice(address)
    } catch (t: Throwable) {
        Log.w(TAG, "deviceFromAddress: ${t.message}")
        null
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasScanPermission()) return
        val a = adapter ?: return
        if (!a.isEnabled) return
        if (_scanning.value) return
        synchronized(seen) {
            seen.clear()
            seen.putAll(pairedDevices().associateBy { it.address })
        }
        _scanResults.value = sortedView()

        // Register classic receiver
        if (!classicReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(classicReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(classicReceiver, filter)
            }
            classicReceiverRegistered = true
        }

        // Start Classic discovery (finds Macs, PCs, TVs that aren't BLE-advertising)
        try {
            if (a.isDiscovering) a.cancelDiscovery()
            a.startDiscovery()
        } catch (t: Throwable) {
            Log.w(TAG, "startDiscovery: ${t.message}")
        }

        // Also start BLE scan in parallel
        scanner = a.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner?.startScan(null, settings, scanCallback)
        } catch (t: Throwable) {
            Log.w(TAG, "startScan(BLE): ${t.message}")
        }
        _scanning.value = true
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_scanning.value) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (t: Throwable) {
            Log.w(TAG, "stopScan(BLE): ${t.message}")
        }
        try {
            if (adapter?.isDiscovering == true) adapter.cancelDiscovery()
        } catch (t: Throwable) {
            Log.w(TAG, "cancelDiscovery: ${t.message}")
        }
        if (classicReceiverRegistered) {
            try { context.unregisterReceiver(classicReceiver) } catch (_: Throwable) {}
            classicReceiverRegistered = false
        }
        _scanning.value = false
    }

    private fun sortedView(): List<DiscoveredDevice> {
        val snapshot = synchronized(seen) { seen.values.toList() }
        return snapshot.sortedWith(compareByDescending<DiscoveredDevice> { it.bonded }.thenByDescending { it.rssi })
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val r = result ?: return
            val device = r.device ?: return
            val name = runCatching { device.name }.getOrNull() ?: r.scanRecord?.deviceName ?: "(unknown)"
            val bonded = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
            synchronized(seen) {
                seen[device.address] = DiscoveredDevice(name, device.address, r.rssi, device, bonded)
            }
            scheduleEmit()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed: $errorCode")
        }
    }

    private fun scheduleEmit() {
        if (pendingEmit) return
        pendingEmit = true
        if (emitJob?.isActive == true) return
        emitJob = emitScope.launch {
            while (pendingEmit) {
                pendingEmit = false
                _scanResults.value = sortedView()
                kotlinx.coroutines.delay(emitIntervalMs)
            }
        }
    }

    fun hasConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

    fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true

    companion object {
        private const val TAG = "BluetoothManager"
    }
}
