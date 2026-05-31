package com.btremote.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.BluetoothManager
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.data.HostProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickPairTarget(val name: String, val address: String)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val controller: HidServiceController,
    private val btManager: BluetoothManager,
    private val profileRepo: HostProfileRepository
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = controller.connectionState

    val quickPairTarget: StateFlow<QuickPairTarget?> = profileRepo.state
        .map { s ->
            val a = s.active ?: return@map null
            if (a.lastDeviceAddress.isBlank()) null
            else QuickPairTarget(
                name = a.lastDeviceName.ifBlank { a.name },
                address = a.lastDeviceAddress
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun quickPair() {
        viewModelScope.launch {
            val a = profileRepo.state.first().active ?: return@launch
            val addr = a.lastDeviceAddress.takeIf { it.isNotBlank() } ?: return@launch
            if (!btManager.isBluetoothEnabled()) return@launch
            val device = btManager.deviceFromAddress(addr) ?: return@launch
            controller.connect(device)
        }
    }
}
