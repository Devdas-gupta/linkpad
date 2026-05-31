package com.btremote.app.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.BluetoothManager
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.DiscoveredDevice
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.data.HostProfileRepository
import com.btremote.app.data.HostProfilesState
import com.btremote.app.data.PairedDeviceEntry
import com.btremote.app.data.PairedDeviceRepository
import com.btremote.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val btManager: BluetoothManager,
    private val controller: HidServiceController,
    private val pairedRepo: PairedDeviceRepository,
    private val prefs: PreferencesRepository,
    private val profileRepo: HostProfileRepository
) : ViewModel() {

    val scanResults: StateFlow<List<DiscoveredDevice>> = btManager.scanResults
    val scanning: StateFlow<Boolean> = btManager.scanning
    val connectionState: StateFlow<ConnectionState> = controller.connectionState
    val appRegistered: StateFlow<Boolean> = controller.appRegistered

    private val _bluetoothEnabled = MutableStateFlow(btManager.isBluetoothEnabled())
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    val pairedHistory: StateFlow<List<PairedDeviceEntry>> = pairedRepo.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profilesState: StateFlow<HostProfilesState?> = profileRepo.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @Volatile
    private var autoConnectTried = false

    init {
        viewModelScope.launch {
            controller.appRegistered.collect { ready ->
                if (ready && !autoConnectTried) {
                    val state = controller.connectionState.value
                    if (state is ConnectionState.Idle || state is ConnectionState.Disconnected) {
                        tryAutoConnect()
                    }
                }
            }
        }
    }

    fun refreshBluetoothEnabled() {
        _bluetoothEnabled.value = btManager.isBluetoothEnabled()
    }

    fun startScan() = btManager.startScan()
    fun stopScan() = btManager.stopScan()

    fun supportsBluetooth(): Boolean = btManager.isBluetoothSupported()

    private suspend fun tryAutoConnect() {
        autoConnectTried = true
        val active = profileRepo.state.first().active
        val addr = active?.lastDeviceAddress?.takeIf { it.isNotBlank() }
            ?: prefs.preferences.first().lastConnectedDeviceAddress
        if (addr.isBlank()) return
        if (!btManager.isBluetoothEnabled()) return
        val device = btManager.deviceFromAddress(addr) ?: return
        controller.connect(device)
    }

    fun connect(device: DiscoveredDevice) {
        controller.connect(device.raw)
        viewModelScope.launch {
            pairedRepo.addOrPromote(PairedDeviceEntry(device.name, device.address))
            prefs.setLastConnectedDeviceAddress(device.address)
            val active = profileRepo.state.first().active ?: return@launch
            profileRepo.setLastDevice(active.id, device.address, device.name)
        }
    }

    fun reconnect(entry: PairedDeviceEntry) {
        val device = btManager.deviceFromAddress(entry.address) ?: return
        controller.connect(device)
        viewModelScope.launch {
            pairedRepo.addOrPromote(entry)
            prefs.setLastConnectedDeviceAddress(entry.address)
            val active = profileRepo.state.first().active ?: return@launch
            profileRepo.setLastDevice(active.id, entry.address, entry.name)
        }
    }

    fun quickReconnect() {
        viewModelScope.launch {
            val active = profileRepo.state.first().active ?: return@launch
            val addr = active.lastDeviceAddress.takeIf { it.isNotBlank() } ?: return@launch
            val device = btManager.deviceFromAddress(addr) ?: return@launch
            controller.connect(device)
        }
    }

    fun disconnect() = controller.disconnect()

    fun forget(entry: PairedDeviceEntry) {
        viewModelScope.launch { pairedRepo.remove(entry.address) }
    }
}
