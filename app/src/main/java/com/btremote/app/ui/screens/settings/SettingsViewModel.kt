package com.btremote.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.HostProfileRepository
import com.btremote.app.data.HostProfilesState
import com.btremote.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val profileRepo: HostProfileRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT)

    val profilesState: StateFlow<HostProfilesState> = profileRepo.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HostProfilesState.DEFAULT)

    fun setPointerSpeed(v: Int) = viewModelScope.launch { prefs.setPointerSpeed(v) }
    fun setScrollSpeed(v: Int) = viewModelScope.launch { prefs.setScrollSpeed(v) }
    fun setAirMouseEnabled(v: Boolean) = viewModelScope.launch { prefs.setAirMouseEnabled(v) }
    fun setAirMouseSensitivity(v: Int) = viewModelScope.launch { prefs.setAirMouseSensitivity(v) }
    fun setScrollBarPosition(v: String) = viewModelScope.launch { prefs.setScrollBarPosition(v) }
    fun setMouseButtonLayout(v: String) = viewModelScope.launch { prefs.setMouseButtonLayout(v) }
    fun setMouseButtonsPosition(v: String) = viewModelScope.launch { prefs.setMouseButtonsPosition(v) }
    fun setShowShortcutsWin(v: Boolean) = viewModelScope.launch { prefs.setShowShortcutsWin(v) }
    fun setShowShortcutsMac(v: Boolean) = viewModelScope.launch { prefs.setShowShortcutsMac(v) }
    fun setShowFKeys(v: Boolean) = viewModelScope.launch { prefs.setShowFKeys(v) }
    fun setShowArrows(v: Boolean) = viewModelScope.launch { prefs.setShowArrows(v) }
    fun setShowEdit(v: Boolean) = viewModelScope.launch { prefs.setShowEdit(v) }
    fun setShowMediaButtons(v: Boolean) = viewModelScope.launch { prefs.setShowMediaButtons(v) }
    fun setThemeMode(v: String) = viewModelScope.launch { prefs.setThemeMode(v) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { prefs.setKeepScreenOn(v) }
    fun setShowOverLockScreen(v: Boolean) = viewModelScope.launch { prefs.setShowOverLockScreen(v) }
    fun setTouchVibrations(v: Boolean) = viewModelScope.launch { prefs.setTouchVibrations(v) }
    fun setVolumeButtonAction(v: String) = viewModelScope.launch { prefs.setVolumeButtonAction(v) }
    fun setDirectInputMode(v: Boolean) = viewModelScope.launch { prefs.setDirectInputMode(v) }
    fun setFullscreenMode(v: Boolean) = viewModelScope.launch { prefs.setFullscreenMode(v) }
    fun setShowAndroidNavButtons(v: Boolean) = viewModelScope.launch { prefs.setShowAndroidNavButtons(v) }
    fun setAirMouseInvert(v: Boolean) = viewModelScope.launch { prefs.setAirMouseInvert(v) }
    fun setAirMouseShowLeft(v: Boolean) = viewModelScope.launch { prefs.setAirMouseShowLeft(v) }
    fun setAirMouseShowRight(v: Boolean) = viewModelScope.launch { prefs.setAirMouseShowRight(v) }
    fun setAirMouseShowMiddle(v: Boolean) = viewModelScope.launch { prefs.setAirMouseShowMiddle(v) }
    fun setAirMouseShowReset(v: Boolean) = viewModelScope.launch { prefs.setAirMouseShowReset(v) }
    fun setTargetOs(v: String) = viewModelScope.launch { prefs.setTargetOs(v) }
    fun setBackgroundServiceNotification(v: Boolean) = viewModelScope.launch { prefs.setBackgroundServiceNotification(v) }

    fun setActiveProfile(id: String) = viewModelScope.launch { profileRepo.setActive(id) }
    fun setProfileTargetOs(id: String, os: String) = viewModelScope.launch { profileRepo.setTargetOs(id, os) }
    fun renameProfile(id: String, name: String) = viewModelScope.launch { profileRepo.rename(id, name) }
    fun addProfile(name: String, os: String) = viewModelScope.launch { profileRepo.addProfile(name, os) }
    fun removeProfile(id: String) = viewModelScope.launch { profileRepo.removeProfile(id) }
    fun setShowCustomShortcuts(v: Boolean) = viewModelScope.launch { prefs.setShowCustomShortcuts(v) }
}
