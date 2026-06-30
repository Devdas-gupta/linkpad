package com.btremote.app.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * BUG 22 — Boot receiver: restarts HidService after device reboot so the app
 * behaves like a real Bluetooth keyboard that auto-connects on power-on.
 *
 * Triggered by BOOT_COMPLETED and MY_PACKAGE_REPLACED (app update).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.i(TAG, "Boot/update received — starting HidService")
        val serviceIntent = Intent(context, HidService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start HidService from boot: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
