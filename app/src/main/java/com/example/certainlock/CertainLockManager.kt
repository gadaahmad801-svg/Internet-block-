package com.example.certainlock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSelected: Boolean = false
)

object CertainLockManager {
    private const val TAG = "CertainLockManager"
    private const val REDIRECT_DEBOUNCE_MS = 1200L

    private val _isLockActive = MutableStateFlow(false)
    val isLockActive: StateFlow<Boolean> = _isLockActive.asStateFlow()

    private val _lockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val lockedPackages: StateFlow<Set<String>> = _lockedPackages.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _redirectCount = MutableStateFlow(0L)
    val redirectCount: StateFlow<Long> = _redirectCount.asStateFlow()

    private val _lastRedirectedApp = MutableStateFlow<String?>(null)
    val lastRedirectedApp: StateFlow<String?> = _lastRedirectedApp.asStateFlow()

    private var lastRedirectTimestamp: Long = 0L
    private var lastRedirectedPkgName: String? = null

    fun updateActiveState(active: Boolean) {
        _isLockActive.value = active
    }

    fun setLockedPackages(packages: Set<String>) {
        _lockedPackages.value = packages
        // Update selection in installed apps
        _installedApps.value = _installedApps.value.map { app ->
            app.copy(isSelected = packages.contains(app.packageName))
        }
    }

    fun toggleAppLocked(packageName: String): Set<String> {
        val current = _lockedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _lockedPackages.value = current
        _installedApps.value = _installedApps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(isSelected = current.contains(packageName))
            } else {
                app
            }
        }
        return current
    }

    fun removeLockedApp(packageName: String): Set<String> {
        val current = _lockedPackages.value.toMutableSet()
        current.remove(packageName)
        _lockedPackages.value = current
        _installedApps.value = _installedApps.value.map { app ->
            if (app.packageName == packageName) app.copy(isSelected = false) else app
        }
        return current
    }

    suspend fun loadInstalledApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedList = pm.queryIntentActivities(mainIntent, 0)
        val currentLocked = _lockedPackages.value
        val myPackage = context.packageName

        val apps = resolvedList
            .mapNotNull { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                // Exclude SULTAN BLOCK itself so user never locks out of control center
                if (pkgName == myPackage) return@mapNotNull null

                val label = resolveInfo.loadLabel(pm)?.toString() ?: pkgName
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                InstalledAppItem(
                    packageName = pkgName,
                    appName = label,
                    icon = icon,
                    isSelected = currentLocked.contains(pkgName)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        _installedApps.value = apps
        apps
    }

    /**
     * Checks if a package should be blocked and redirected to home screen.
     * Implements debounce to avoid infinite loop when Home launcher appears.
     */
    fun shouldRedirect(packageName: String?, context: Context): Boolean {
        if (!_isLockActive.value) return false
        if (packageName.isNullOrEmpty()) return false
        if (packageName == context.packageName) return false

        // Never redirect system launcher / system UI
        if (packageName == "com.android.systemui" || packageName == "com.google.android.apps.nexuslauncher") {
            return false
        }

        if (!_lockedPackages.value.contains(packageName)) {
            return false
        }

        val now = SystemClock.elapsedRealtime()
        if (packageName == lastRedirectedPkgName && (now - lastRedirectTimestamp) < REDIRECT_DEBOUNCE_MS) {
            // Debounce active
            return false
        }

        lastRedirectTimestamp = now
        lastRedirectedPkgName = packageName
        _redirectCount.value += 1
        _lastRedirectedApp.value = packageName

        Log.i(TAG, "Certain Lock intercepted restricted app: $packageName. Returning to Home Screen.")
        return true
    }

    fun returnToHomeViaIntent(context: Context) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch home intent: ${e.message}")
        }
    }
}
