package com.btremote.app.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class HidServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    val reportSender: HidReportSender
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _appRegistered = MutableStateFlow(false)
    val appRegistered: StateFlow<Boolean> = _appRegistered.asStateFlow()

    @Volatile
    private var service: HidService? = null
    @Volatile
    private var bound: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var stateJob: Job? = null
    private var registeredJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? HidService.LocalBinder ?: return
            val svc = localBinder.getService()
            service = svc
            bound = true
            stateJob?.cancel()
            registeredJob?.cancel()
            stateJob = scope.launch {
                svc.connectionState.collect { _connectionState.value = it }
            }
            registeredJob = scope.launch {
                svc.appRegistered.collect { _appRegistered.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
            stateJob?.cancel()
            registeredJob?.cancel()
        }
    }

    fun start(backgroundRun: Boolean) {
        val intent = Intent(context, HidService::class.java)
        if (backgroundRun && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        if (!bound) {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun stop() {
        if (bound) {
            try {
                context.unbindService(connection)
            } catch (_: Throwable) { }
            bound = false
        }
        context.stopService(Intent(context, HidService::class.java))
    }

    fun connect(device: android.bluetooth.BluetoothDevice) {
        service?.connectToDevice(device)
    }

    fun disconnect() {
        service?.disconnectCurrent()
    }
}
