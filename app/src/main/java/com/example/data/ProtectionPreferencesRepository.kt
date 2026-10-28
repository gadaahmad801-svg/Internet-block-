package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.protectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "protection_toggle_preferences")

data class ProtectionToggleStates(
    val isInternetBlockEnabled: Boolean = false,
    val isVolumeLockEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isLockMediaOnly: Boolean = false,
    val lockedAppPackages: Set<String> = emptySet()
)

/**
 * DataStore repository dedicated to persisting the user's toggle states
 * and configuration for Internet Blocking, Volume Lock, and App Lock (Certain Lock).
 */
class ProtectionPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_INTERNET_BLOCK_ENABLED = booleanPreferencesKey("pref_internet_block_enabled")
        val KEY_VOLUME_LOCK_ENABLED = booleanPreferencesKey("pref_volume_lock_enabled")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("pref_app_lock_enabled")
        val KEY_LOCK_MEDIA_ONLY = booleanPreferencesKey("pref_lock_media_only")
        val KEY_LOCKED_APP_PACKAGES = stringSetPreferencesKey("pref_locked_app_packages")
    }

    val toggleStates: Flow<ProtectionToggleStates> = context.protectionDataStore.data.map { prefs ->
        ProtectionToggleStates(
            isInternetBlockEnabled = prefs[KEY_INTERNET_BLOCK_ENABLED] ?: false,
            isVolumeLockEnabled = prefs[KEY_VOLUME_LOCK_ENABLED] ?: false,
            isAppLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            isLockMediaOnly = prefs[KEY_LOCK_MEDIA_ONLY] ?: false,
            lockedAppPackages = prefs[KEY_LOCKED_APP_PACKAGES] ?: emptySet()
        )
    }

    val isInternetBlockEnabled: Flow<Boolean> = context.protectionDataStore.data.map { prefs ->
        prefs[KEY_INTERNET_BLOCK_ENABLED] ?: false
    }

    val isVolumeLockEnabled: Flow<Boolean> = context.protectionDataStore.data.map { prefs ->
        prefs[KEY_VOLUME_LOCK_ENABLED] ?: false
    }

    val isAppLockEnabled: Flow<Boolean> = context.protectionDataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val isLockMediaOnly: Flow<Boolean> = context.protectionDataStore.data.map { prefs ->
        prefs[KEY_LOCK_MEDIA_ONLY] ?: false
    }

    val lockedAppPackages: Flow<Set<String>> = context.protectionDataStore.data.map { prefs ->
        prefs[KEY_LOCKED_APP_PACKAGES] ?: emptySet()
    }

    suspend fun setInternetBlockEnabled(enabled: Boolean) {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_INTERNET_BLOCK_ENABLED] = enabled
        }
    }

    suspend fun setVolumeLockEnabled(enabled: Boolean) {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_VOLUME_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockMediaOnly(mediaOnly: Boolean) {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_LOCK_MEDIA_ONLY] = mediaOnly
        }
    }

    suspend fun setLockedAppPackages(packages: Set<String>) {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_LOCKED_APP_PACKAGES] = packages
        }
    }

    suspend fun addLockedAppPackage(packageName: String) {
        context.protectionDataStore.edit { prefs ->
            val current = prefs[KEY_LOCKED_APP_PACKAGES] ?: emptySet()
            prefs[KEY_LOCKED_APP_PACKAGES] = current + packageName
        }
    }

    suspend fun removeLockedAppPackage(packageName: String) {
        context.protectionDataStore.edit { prefs ->
            val current = prefs[KEY_LOCKED_APP_PACKAGES] ?: emptySet()
            prefs[KEY_LOCKED_APP_PACKAGES] = current - packageName
        }
    }

    suspend fun clearAllToggles() {
        context.protectionDataStore.edit { prefs ->
            prefs[KEY_INTERNET_BLOCK_ENABLED] = false
            prefs[KEY_VOLUME_LOCK_ENABLED] = false
            prefs[KEY_APP_LOCK_ENABLED] = false
        }
    }
}
