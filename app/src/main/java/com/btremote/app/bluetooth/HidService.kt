package com.btremote.app.bluetooth

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.btremote.app.MainActivity
import com.btremote.app.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import com.btremote.app.data.PreferencesRepository

@AndroidEntryPoint
class HidService : Service() {

    @Inject lateinit var reportSender: HidReportSender
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val binder = LocalBinder()
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var isForeground = false
    @Volatile
    private var lastNotificationText = ""

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _appRegistered = MutableStateFlow(false)
    val appRegistered: StateFlow<Boolean> = _appRegistered.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var pendingTarget: BluetoothDevice? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var btStateReceiverRegistered = false

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.i(TAG, "BT STATE_ON — registering HID proxy")
                            if (hidDevice == null) registerHidProxy()
                        }
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                            _appRegistered.value = false
                            _connectionState.value = ConnectionState.Disconnected
                            reportSender.attach(null, null)
                            hidDevice = null
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device ?: return
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                    Log.i(TAG, "Bond state changed for ${device.address}: prev=$prevBondState new=$bondState")
                    
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        val state = _connectionState.value
                        if (state is ConnectionState.Connecting && state.device.address == device.address) {
                            Log.i(TAG, "Device bonded successfully. Connecting HID...")
                            val proxy = hidDevice
                            if (proxy != null && _appRegistered.value) {
                                try {
                                    proxy.connect(device)
                                } catch (t: Throwable) {
                                    _connectionState.value = ConnectionState.Error("connect post-bond: ${t.message}")
                                }
                            }
                        }
                    } else if (bondState == BluetoothDevice.BOND_NONE && prevBondState == BluetoothDevice.BOND_BONDING) {
                        val state = _connectionState.value
                        if (state is ConnectionState.Connecting && state.device.address == device.address) {
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        bluetoothAdapter = manager?.adapter
        createNotificationChannel()
        if (!hasBluetoothConnectPermission()) {
            Log.w(TAG, "BLUETOOTH_CONNECT not granted; stopping HidService")
            stopSelf()
            return
        }

        lastNotificationText = getString(R.string.notification_text_disconnected)

        // Observe preference and connection state to manage foreground service status
        serviceScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            combine(
                preferencesRepository.preferences,
                _connectionState
            ) { prefs, connState ->
                prefs.backgroundServiceNotification || connState is ConnectionState.Connected
            }.collect { shouldBeForeground ->
                if (shouldBeForeground) {
                    if (!isForeground) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    NOTIFICATION_ID,
                                    buildNotification(lastNotificationText),
                                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                                )
                            } else {
                                startForeground(NOTIFICATION_ID, buildNotification(lastNotificationText))
                            }
                            isForeground = true
                            Log.i(TAG, "Promoted to foreground service")
                        } catch (t: Throwable) {
                            Log.e(TAG, "startForeground failed: ${t.message}")
                        }
                    }
                } else {
                    if (isForeground) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                        isForeground = false
                        Log.i(TAG, "Demoted from foreground service")
                    }
                }
            }
        }

        registerBtStateReceiver()
        if (bluetoothAdapter?.isEnabled == true) {
            registerHidProxy()
        } else {
            Log.i(TAG, "Bluetooth off — waiting for STATE_ON")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasBluetoothConnectPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            hidDevice?.unregisterApp()
        } catch (t: Throwable) {
            Log.w(TAG, "unregisterApp: ${t.message}")
        }
        try {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        } catch (t: Throwable) {
            Log.w(TAG, "closeProfileProxy: ${t.message}")
        }
        if (btStateReceiverRegistered) {
            try { unregisterReceiver(btStateReceiver) } catch (_: Throwable) {}
            btStateReceiverRegistered = false
        }
        hidDevice = null
        executor.shutdownNow()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothConnectPermission()) {
            _connectionState.value = ConnectionState.Error("Missing BLUETOOTH_CONNECT permission")
            return
        }
        val bond = device.bondState
        if (bond != BluetoothDevice.BOND_BONDED) {
            ensureDiscoverable()
        }
        val proxy = hidDevice
        if (proxy == null) {
            pendingTarget = device
            _connectionState.value = ConnectionState.Connecting(device)
            if (bluetoothAdapter?.isEnabled == true) registerHidProxy()
            return
        }
        if (!_appRegistered.value) {
            pendingTarget = device
            _connectionState.value = ConnectionState.Connecting(device)
            return
        }
        try {
            Log.i(TAG, "connectToDevice: addr=${device.address} bond=$bond")
            _connectionState.value = ConnectionState.Connecting(device)
            if (bond != BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device not bonded. Creating bond first...")
                val success = device.createBond()
                if (!success) {
                    _connectionState.value = ConnectionState.Error("createBond returned false")
                }
            } else {
                proxy.connect(device)
            }
        } catch (t: Throwable) {
            _connectionState.value = ConnectionState.Error("connect: ${t.message}")
        }
    }

    fun disconnectCurrent() {
        val proxy = hidDevice ?: return
        val state = _connectionState.value
        if (state is ConnectionState.Connected) {
            try {
                proxy.disconnect(state.device)
            } catch (t: Throwable) {
                Log.w(TAG, "disconnect: ${t.message}")
            }
        }
    }

    private fun registerBtStateReceiver() {
        if (btStateReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(btStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(btStateReceiver, filter)
        }
        btStateReceiverRegistered = true
    }

    private fun ensureDiscoverable() {
        val adapter = bluetoothAdapter ?: return
        try {
            if (adapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ensureDiscoverable: ${t.message}")
        }
    }

    private fun registerHidProxy() {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
            return
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "registerHidProxy: adapter disabled")
            return
        }
        if (!hasBluetoothConnectPermission()) {
            _connectionState.value = ConnectionState.Error("Missing BLUETOOTH_CONNECT permission")
            return
        }
        try {
            adapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
        } catch (t: Throwable) {
            _connectionState.value = ConnectionState.Error("Profile proxy failed: ${t.message}")
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val hid = proxy as? BluetoothHidDevice ?: return
            hidDevice = hid
            registerApp(hid)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _appRegistered.value = false
                _connectionState.value = ConnectionState.Disconnected
                reportSender.attach(null, null)
            }
        }
    }

    private fun registerApp(proxy: BluetoothHidDevice) {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            HidDescriptors.SDP_NAME,
            HidDescriptors.SDP_DESCRIPTION,
            HidDescriptors.SDP_PROVIDER,
            HidDescriptors.SUBCLASS,
            HidDescriptors.COMBINED_DESCRIPTOR
        )
        // QoS values match the commercial Bluetooth Keyboard & Mouse APK known to work
        // on Pixel/Samsung. Null QoS works on AOSP samples but some skinned Android
        // builds drop sendReport silently without populated QoS.
        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            /* tokenRate */ 6000,
            /* tokenBucketSize */ 1024,
            /* peakBandwidth */ 10000,
            /* latency */ 11250,
            /* delayVariation */ 11250
        )
        try {
            proxy.registerApp(sdp, qos, qos, executor, callback)
        } catch (t: Throwable) {
            Log.e(TAG, "registerApp failed: ${t.message}", t)
            _connectionState.value = ConnectionState.Error("registerApp: ${t.message}")
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.i(TAG, "onAppStatusChanged: registered=$registered plugged=${pluggedDevice?.address}")
            _appRegistered.value = registered
            if (!registered) {
                reportSender.attach(null, null)
                return
            }
            // Prefer plugged device from system, fall back to pending target
            val target = pluggedDevice ?: pendingTarget
            if (target != null) {
                val proxy = hidDevice
                if (proxy != null) {
                    val curState = try { proxy.getConnectionState(target) } catch (_: Throwable) { BluetoothProfile.STATE_DISCONNECTED }
                    Log.i(TAG, "post-register state=$curState for ${target.address}")
                    if (curState == BluetoothProfile.STATE_CONNECTED) {
                        lastNotificationText = buildConnectedText(target)
                        _connectionState.value = ConnectionState.Connected(target)
                        reportSender.attach(proxy, target)
                        updateNotification(lastNotificationText)
                    } else if (curState != BluetoothProfile.STATE_CONNECTING) {
                        try {
                            proxy.connect(target)
                        } catch (t: Throwable) {
                            _connectionState.value = ConnectionState.Error("connect: ${t.message}")
                        }
                    }
                }
                pendingTarget = null
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            device ?: return
            Log.i(TAG, "onConnectionStateChanged: ${device.address} state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    lastNotificationText = buildConnectedText(device)
                    _connectionState.value = ConnectionState.Connected(device)
                    reportSender.attach(hidDevice, device)
                    updateNotification(lastNotificationText)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.Connecting(device)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    lastNotificationText = getString(R.string.notification_text_disconnected)
                    if (_connectionState.value is ConnectionState.Connected ||
                        _connectionState.value is ConnectionState.Connecting) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    reportSender.attach(null, null)
                    updateNotification(lastNotificationText)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            val size = when (id) {
                HidDescriptors.REPORT_ID_MOUSE -> 5
                HidDescriptors.REPORT_ID_KEYBOARD -> 8
                HidDescriptors.REPORT_ID_CONSUMER -> 2
                else -> bufferSize.coerceAtLeast(1)
            }
            try {
                hidDevice?.replyReport(device, type, id, ByteArray(size))
            } catch (t: Throwable) {
                Log.w(TAG, "replyReport: ${t.message}")
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            // Host LED state (CapsLock/NumLock) — accept silently
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            super.onVirtualCableUnplug(device)
            Log.i(TAG, "onVirtualCableUnplug: ${device?.address}")
            lastNotificationText = getString(R.string.notification_text_disconnected)
            _connectionState.value = ConnectionState.Disconnected
            reportSender.attach(null, null)
            updateNotification(lastNotificationText)
        }
    }

    private fun buildConnectedText(device: BluetoothDevice): String {
        val name = runCatching { device.name }.getOrNull() ?: device.address
        return getString(R.string.notification_text_connected, name)
    }

    private fun updateNotification(text: String) {
        lastNotificationText = text
        if (isForeground) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

    companion object {
        private const val TAG = "HidService"
        private const val CHANNEL_ID = "bt_remote_hid_channel"
        private const val NOTIFICATION_ID = 0xB7E
    }
}
