package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sultan_block_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_RESTORE_INTERNET_BOOT = booleanPreferencesKey("restore_internet_boot")
        val KEY_RESTORE_VOLUME_BOOT = booleanPreferencesKey("restore_volume_boot")
        val KEY_LOCK_MEDIA_ONLY = booleanPreferencesKey("lock_media_only")
        val KEY_STRICT_DNS_SINKHOLE = booleanPreferencesKey("strict_dns_sinkhole")
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
}
