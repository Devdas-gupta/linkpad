package com.btremote.app.bluetooth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Quick Settings tile — appears in the Android notification shade pulldown.
 * State: ACTIVE = Bluetooth HID connected, INACTIVE = not connected.
 * Tapping opens the app (or reconnects if already running).
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class LinkpadQsTileService : TileService() {

    @Inject lateinit var controller: HidServiceController

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = buildLaunchIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
        updateTile()
    }

    private fun buildLaunchIntent(): Intent =
        packageManager.getLaunchIntentForPackage(packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            } ?: Intent()

    private fun updateTile() {
        val tile = qsTile ?: return
        val connected = controller.connectionState.value is ConnectionState.Connected
        val deviceName = (controller.connectionState.value as? ConnectionState.Connected)
            ?.let { runCatching { it.device.name }.getOrNull() } ?: ""

        tile.state   = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label   = "Linkpad"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            tile.subtitle = if (connected) deviceName else "Not connected"
        }
        tile.updateTile()
    }
}
