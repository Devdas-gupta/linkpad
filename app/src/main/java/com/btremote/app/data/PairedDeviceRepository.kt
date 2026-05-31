package com.btremote.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pairedDeviceDataStore by preferencesDataStore(name = "bt_remote_paired_devices")

data class PairedDeviceEntry(val name: String, val address: String)

@Singleton
class PairedDeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.pairedDeviceDataStore
    private val key = stringPreferencesKey("paired_device_history")
    private val maxEntries = 5
    private val separator = "||"
    private val pairSeparator = "|@|"

    val devices: Flow<List<PairedDeviceEntry>> = dataStore.data.map { prefs ->
        decode(prefs[key] ?: "")
    }

    suspend fun addOrPromote(entry: PairedDeviceEntry) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key] ?: "").toMutableList()
            current.removeAll { it.address == entry.address }
            current.add(0, entry)
            while (current.size > maxEntries) current.removeAt(current.size - 1)
            prefs[key] = encode(current)
        }
    }

    suspend fun remove(address: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key] ?: "").filterNot { it.address == address }
            prefs[key] = encode(current)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs[key] = "" }
    }

    private fun encode(list: List<PairedDeviceEntry>): String =
        list.joinToString(separator) { "${it.name}$pairSeparator${it.address}" }

    private fun decode(raw: String): List<PairedDeviceEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(separator).mapNotNull { chunk ->
            val parts = chunk.split(pairSeparator)
            if (parts.size == 2 && parts[1].isNotBlank()) {
                PairedDeviceEntry(parts[0], parts[1])
            } else null
        }
    }
}
