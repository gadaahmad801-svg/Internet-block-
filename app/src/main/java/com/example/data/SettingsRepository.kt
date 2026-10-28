package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pin.PinProtectionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sultan_block_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_RESTORE_INTERNET_BOOT = booleanPreferencesKey("restore_internet_boot")
        val KEY_RESTORE_VOLUME_BOOT = booleanPreferencesKey("restore_volume_boot")
        val KEY_LOCK_MEDIA_ONLY = booleanPreferencesKey("lock_media_only")
        val KEY_STRICT_DNS_SINKHOLE = booleanPreferencesKey("strict_dns_sinkhole")

        val KEY_SCHEDULES_JSON = stringPreferencesKey("schedules_json")
        val KEY_TIMER_JSON = stringPreferencesKey("timer_json")

        val KEY_LOCKED_PACKAGES = stringSetPreferencesKey("locked_packages")
        val KEY_CERTAIN_LOCK_ACTIVE = booleanPreferencesKey("certain_lock_active")

        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_RECOVERY_HASH = stringPreferencesKey("recovery_hash")
        val KEY_RECOVERY_SALT = stringPreferencesKey("recovery_salt")

        val KEY_PROTECT_SETTINGS = booleanPreferencesKey("protect_settings")
        val KEY_PROTECT_INTERNET = booleanPreferencesKey("protect_internet")
        val KEY_PROTECT_VOLUME = booleanPreferencesKey("protect_volume")
        val KEY_PROTECT_SCHEDULE = booleanPreferencesKey("protect_schedule")
        val KEY_PROTECT_TIMER = booleanPreferencesKey("protect_timer")
        val KEY_PROTECT_CERTAIN_LOCK = booleanPreferencesKey("protect_certain_lock")
    }

    val autoRestoreInternetOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RESTORE_INTERNET_BOOT] ?: false
    }

    val autoRestoreVolumeOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RESTORE_VOLUME_BOOT] ?: false
    }

    val lockMediaOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOCK_MEDIA_ONLY] ?: false
    }

    val strictDnsSinkhole: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_STRICT_DNS_SINKHOLE] ?: true
    }

    val schedulesJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SCHEDULES_JSON]
    }

    val timerJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_TIMER_JSON]
    }

    val lockedPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOCKED_PACKAGES] ?: emptySet()
    }

    val isCertainLockActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CERTAIN_LOCK_ACTIVE] ?: false
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_HASH]
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_SALT]
    }

    val recoveryHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_RECOVERY_HASH]
    }

    val recoverySalt: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_RECOVERY_SALT]
    }

    val protectSettings: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_SETTINGS] ?: true }
    val protectInternetBlock: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_INTERNET] ?: false }
    val protectVolumeLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_VOLUME] ?: false }
    val protectSchedule: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_SCHEDULE] ?: false }
    val protectTimer: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_TIMER] ?: false }
    val protectCertainLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROTECT_CERTAIN_LOCK] ?: false }

    suspend fun setRestoreInternetOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RESTORE_INTERNET_BOOT] = enabled
        }
    }

    suspend fun setRestoreVolumeOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RESTORE_VOLUME_BOOT] = enabled
        }
    }

    suspend fun setLockMediaOnly(mediaOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCK_MEDIA_ONLY] = mediaOnly
        }
    }

    suspend fun setStrictDnsSinkhole(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STRICT_DNS_SINKHOLE] = enabled
        }
    }

    suspend fun saveSchedulesJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCHEDULES_JSON] = json
        }
    }

    suspend fun saveTimerJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TIMER_JSON] = json
        }
    }

    suspend fun saveLockedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCKED_PACKAGES] = packages
        }
    }

    suspend fun saveCertainLockActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CERTAIN_LOCK_ACTIVE] = active
        }
    }

    suspend fun savePin(hash: String, salt: String, recoveryHash: String, recoverySalt: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_HASH] = hash
            preferences[KEY_PIN_SALT] = salt
            preferences[KEY_RECOVERY_HASH] = recoveryHash
            preferences[KEY_RECOVERY_SALT] = recoverySalt
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PIN_HASH)
            preferences.remove(KEY_PIN_SALT)
            preferences.remove(KEY_RECOVERY_HASH)
            preferences.remove(KEY_RECOVERY_SALT)
        }
    }

    suspend fun savePinConfig(config: PinProtectionConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROTECT_SETTINGS] = config.protectSettings
            preferences[KEY_PROTECT_INTERNET] = config.protectInternetBlock
            preferences[KEY_PROTECT_VOLUME] = config.protectVolumeLock
            preferences[KEY_PROTECT_SCHEDULE] = config.protectSchedule
            preferences[KEY_PROTECT_TIMER] = config.protectTimer
            preferences[KEY_PROTECT_CERTAIN_LOCK] = config.protectCertainLock
        }
    }
}
