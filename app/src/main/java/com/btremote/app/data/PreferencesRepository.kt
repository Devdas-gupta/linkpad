package com.btremote.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore(name = "bt_remote_prefs")

data class AppPreferences(
    val pointerSpeed: Int,
    val scrollSpeed: Int,
    val airMouseEnabled: Boolean,
    val airMouseSensitivity: Int,
    val airMouseInvert: Boolean,
    val airMouseShowLeft: Boolean,
    val airMouseShowRight: Boolean,
    val airMouseShowMiddle: Boolean,
    val airMouseShowReset: Boolean,
    val scrollBarPosition: String,
    val mouseButtonLayout: String,
    val mouseButtonsPosition: String,
    val showShortcuts: Boolean,
    val showFKeys: Boolean,
    val showArrows: Boolean,
    val showEdit: Boolean,
    val showMediaButtons: Boolean,
    val themeMode: String,
    val keepScreenOn: Boolean,
    val showOverLockScreen: Boolean,
    val touchVibrations: Boolean,
    val volumeButtonAction: String,
    val lastConnectedDeviceAddress: String,
    val directInputMode: Boolean,
    val fullscreenMode: Boolean,
    val showAndroidNavButtons: Boolean,
    val targetOs: String
)

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.preferencesDataStore

    private object Keys {
        val POINTER_SPEED = intPreferencesKey("pointer_speed")
        val SCROLL_SPEED = intPreferencesKey("scroll_speed")
        val AIR_MOUSE_ENABLED = booleanPreferencesKey("air_mouse_enabled")
        val AIR_MOUSE_SENSITIVITY = intPreferencesKey("air_mouse_sensitivity")
        val AIR_MOUSE_INVERT = booleanPreferencesKey("air_mouse_invert")
        val AIR_MOUSE_SHOW_LEFT = booleanPreferencesKey("air_mouse_show_left")
        val AIR_MOUSE_SHOW_RIGHT = booleanPreferencesKey("air_mouse_show_right")
        val AIR_MOUSE_SHOW_MIDDLE = booleanPreferencesKey("air_mouse_show_middle")
        val AIR_MOUSE_SHOW_RESET = booleanPreferencesKey("air_mouse_show_reset")
        val SCROLL_BAR_POSITION = stringPreferencesKey("scroll_bar_position")
        val MOUSE_BUTTON_LAYOUT = stringPreferencesKey("mouse_button_layout")
        val MOUSE_BUTTONS_POSITION = stringPreferencesKey("mouse_buttons_position")
        val SHOW_SHORTCUTS = booleanPreferencesKey("show_shortcuts")
        val SHOW_FKEYS = booleanPreferencesKey("show_fkeys")
        val SHOW_ARROWS = booleanPreferencesKey("show_arrows")
        val SHOW_EDIT = booleanPreferencesKey("show_edit")
        val SHOW_MEDIA_BUTTONS = booleanPreferencesKey("show_media_buttons")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_OVER_LOCK_SCREEN = booleanPreferencesKey("show_over_lock_screen")
        val TOUCH_VIBRATIONS = booleanPreferencesKey("touch_vibrations")
        val VOLUME_BUTTON_ACTION = stringPreferencesKey("volume_button_action")
        val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_connected_device_address")
        val DIRECT_INPUT_MODE = booleanPreferencesKey("direct_input_mode")
        val FULLSCREEN_MODE = booleanPreferencesKey("fullscreen_mode")
        val SHOW_ANDROID_NAV_BUTTONS = booleanPreferencesKey("show_android_nav_buttons")
        val TARGET_OS = stringPreferencesKey("target_os")
    }

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs -> prefs.toAppPreferences() }

    private fun Preferences.toAppPreferences() = AppPreferences(
        pointerSpeed = this[Keys.POINTER_SPEED] ?: 8,
        scrollSpeed = this[Keys.SCROLL_SPEED] ?: 5,
        airMouseEnabled = this[Keys.AIR_MOUSE_ENABLED] ?: false,
        airMouseSensitivity = this[Keys.AIR_MOUSE_SENSITIVITY] ?: 8,
        airMouseInvert = this[Keys.AIR_MOUSE_INVERT] ?: false,
        airMouseShowLeft = this[Keys.AIR_MOUSE_SHOW_LEFT] ?: true,
        airMouseShowRight = this[Keys.AIR_MOUSE_SHOW_RIGHT] ?: true,
        airMouseShowMiddle = this[Keys.AIR_MOUSE_SHOW_MIDDLE] ?: false,
        airMouseShowReset = this[Keys.AIR_MOUSE_SHOW_RESET] ?: true,
        scrollBarPosition = this[Keys.SCROLL_BAR_POSITION] ?: "right",
        mouseButtonLayout = this[Keys.MOUSE_BUTTON_LAYOUT] ?: "left_right",
        mouseButtonsPosition = this[Keys.MOUSE_BUTTONS_POSITION] ?: "bottom",
        showShortcuts = this[Keys.SHOW_SHORTCUTS] ?: false,
        showFKeys = this[Keys.SHOW_FKEYS] ?: false,
        showArrows = this[Keys.SHOW_ARROWS] ?: false,
        showEdit = this[Keys.SHOW_EDIT] ?: false,
        showMediaButtons = this[Keys.SHOW_MEDIA_BUTTONS] ?: false,
        themeMode = this[Keys.THEME_MODE] ?: "system",
        keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: false,
        showOverLockScreen = this[Keys.SHOW_OVER_LOCK_SCREEN] ?: true,
        touchVibrations = this[Keys.TOUCH_VIBRATIONS] ?: true,
        volumeButtonAction = this[Keys.VOLUME_BUTTON_ACTION] ?: "remote",
        lastConnectedDeviceAddress = this[Keys.LAST_DEVICE_ADDRESS] ?: "",
        directInputMode = this[Keys.DIRECT_INPUT_MODE] ?: false,
        fullscreenMode = this[Keys.FULLSCREEN_MODE] ?: false,
        showAndroidNavButtons = this[Keys.SHOW_ANDROID_NAV_BUTTONS] ?: false,
        targetOs = this[Keys.TARGET_OS] ?: "auto"
    )

    suspend fun setPointerSpeed(value: Int) = dataStore.edit { it[Keys.POINTER_SPEED] = value }
    suspend fun setScrollSpeed(value: Int) = dataStore.edit { it[Keys.SCROLL_SPEED] = value }
    suspend fun setAirMouseEnabled(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_ENABLED] = value }
    suspend fun setAirMouseSensitivity(value: Int) = dataStore.edit { it[Keys.AIR_MOUSE_SENSITIVITY] = value }
    suspend fun setAirMouseInvert(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_INVERT] = value }
    suspend fun setAirMouseShowLeft(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_SHOW_LEFT] = value }
    suspend fun setAirMouseShowRight(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_SHOW_RIGHT] = value }
    suspend fun setAirMouseShowMiddle(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_SHOW_MIDDLE] = value }
    suspend fun setAirMouseShowReset(value: Boolean) = dataStore.edit { it[Keys.AIR_MOUSE_SHOW_RESET] = value }
    suspend fun setScrollBarPosition(value: String) = dataStore.edit { it[Keys.SCROLL_BAR_POSITION] = value }
    suspend fun setMouseButtonLayout(value: String) = dataStore.edit { it[Keys.MOUSE_BUTTON_LAYOUT] = value }
    suspend fun setMouseButtonsPosition(value: String) = dataStore.edit { it[Keys.MOUSE_BUTTONS_POSITION] = value }
    suspend fun setShowShortcuts(value: Boolean) = dataStore.edit { it[Keys.SHOW_SHORTCUTS] = value }
    suspend fun setShowFKeys(value: Boolean) = dataStore.edit { it[Keys.SHOW_FKEYS] = value }
    suspend fun setShowArrows(value: Boolean) = dataStore.edit { it[Keys.SHOW_ARROWS] = value }
    suspend fun setShowEdit(value: Boolean) = dataStore.edit { it[Keys.SHOW_EDIT] = value }
    suspend fun setShowMediaButtons(value: Boolean) = dataStore.edit { it[Keys.SHOW_MEDIA_BUTTONS] = value }
    suspend fun setThemeMode(value: String) = dataStore.edit { it[Keys.THEME_MODE] = value }
    suspend fun setKeepScreenOn(value: Boolean) = dataStore.edit { it[Keys.KEEP_SCREEN_ON] = value }
    suspend fun setShowOverLockScreen(value: Boolean) = dataStore.edit { it[Keys.SHOW_OVER_LOCK_SCREEN] = value }
    suspend fun setTouchVibrations(value: Boolean) = dataStore.edit { it[Keys.TOUCH_VIBRATIONS] = value }
    suspend fun setVolumeButtonAction(value: String) = dataStore.edit { it[Keys.VOLUME_BUTTON_ACTION] = value }
    suspend fun setLastConnectedDeviceAddress(value: String) = dataStore.edit { it[Keys.LAST_DEVICE_ADDRESS] = value }
    suspend fun setDirectInputMode(value: Boolean) = dataStore.edit { it[Keys.DIRECT_INPUT_MODE] = value }
    suspend fun setFullscreenMode(value: Boolean) = dataStore.edit { it[Keys.FULLSCREEN_MODE] = value }
    suspend fun setShowAndroidNavButtons(value: Boolean) = dataStore.edit { it[Keys.SHOW_ANDROID_NAV_BUTTONS] = value }
    suspend fun setTargetOs(value: String) = dataStore.edit { it[Keys.TARGET_OS] = value }
}
