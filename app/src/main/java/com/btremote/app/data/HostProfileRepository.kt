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

private val Context.hostProfileDataStore by preferencesDataStore(name = "bt_remote_host_profiles")

data class HostProfile(
    val id: String,
    val name: String,
    val targetOs: String,
    val lastDeviceAddress: String,
    val lastDeviceName: String
)

data class HostProfilesState(
    val profiles: List<HostProfile>,
    val activeId: String
) {
    val active: HostProfile? get() = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
}

@Singleton
class HostProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.hostProfileDataStore
    private val profilesKey = stringPreferencesKey("host_profiles_v1")
    private val activeKey = stringPreferencesKey("host_profile_active")

    private val rowSep = "||"
    private val fieldSep = "|@|"

    val state: Flow<HostProfilesState> = dataStore.data.map { prefs ->
        val raw = prefs[profilesKey] ?: ""
        val list = decode(raw).ifEmpty { defaultProfiles() }
        val active = prefs[activeKey] ?: list.first().id
        HostProfilesState(list, if (list.any { it.id == active }) active else list.first().id)
    }

    suspend fun setActive(id: String) {
        dataStore.edit { prefs ->
            ensureSeed(prefs)
            prefs[activeKey] = id
        }
    }

    suspend fun setTargetOs(id: String, targetOs: String) = update(id) { it.copy(targetOs = targetOs) }

    suspend fun setLastDevice(id: String, address: String, name: String) =
        update(id) { it.copy(lastDeviceAddress = address, lastDeviceName = name) }

    suspend fun rename(id: String, name: String) = update(id) { it.copy(name = name) }

    suspend fun addProfile(name: String, targetOs: String): String {
        val newId = "p_${System.currentTimeMillis()}"
        dataStore.edit { prefs ->
            val list = decode(prefs[profilesKey] ?: "").ifEmpty { defaultProfiles() }.toMutableList()
            list.add(HostProfile(newId, name, targetOs, "", ""))
            prefs[profilesKey] = encode(list)
            prefs[activeKey] = newId
        }
        return newId
    }

    suspend fun removeProfile(id: String) {
        dataStore.edit { prefs ->
            val list = decode(prefs[profilesKey] ?: "").ifEmpty { defaultProfiles() }.toMutableList()
            if (list.size <= 1) return@edit
            list.removeAll { it.id == id }
            prefs[profilesKey] = encode(list)
            if (prefs[activeKey] == id) prefs[activeKey] = list.first().id
        }
    }

    private suspend fun update(id: String, transform: (HostProfile) -> HostProfile) {
        dataStore.edit { prefs ->
            val list = decode(prefs[profilesKey] ?: "").ifEmpty { defaultProfiles() }.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx < 0) return@edit
            list[idx] = transform(list[idx])
            prefs[profilesKey] = encode(list)
        }
    }

    private fun ensureSeed(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        if ((prefs[profilesKey] ?: "").isBlank()) {
            prefs[profilesKey] = encode(defaultProfiles())
        }
    }

    private fun defaultProfiles(): List<HostProfile> = listOf(
        HostProfile("p_mac", "Mac", "mac", "", ""),
        HostProfile("p_pc", "PC", "windows", "", ""),
        HostProfile("p_ipad", "iPad", "auto", "", "")
    )

    private fun encode(list: List<HostProfile>): String =
        list.joinToString(rowSep) {
            listOf(
                it.id,
                it.name.replace(rowSep, " ").replace(fieldSep, " "),
                it.targetOs,
                it.lastDeviceAddress,
                it.lastDeviceName.replace(rowSep, " ").replace(fieldSep, " ")
            ).joinToString(fieldSep)
        }

    private fun decode(raw: String): List<HostProfile> {
        if (raw.isBlank()) return emptyList()
        return raw.split(rowSep).mapNotNull { row ->
            val parts = row.split(fieldSep)
            if (parts.size >= 5) {
                HostProfile(parts[0], parts[1], parts[2], parts[3], parts[4])
            } else null
        }
    }
}
