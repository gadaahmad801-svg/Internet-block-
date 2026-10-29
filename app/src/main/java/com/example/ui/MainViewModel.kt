package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStreamInfo
import com.example.audio.VolumeLockService
import com.example.audio.VolumeStateManager
import com.example.certainlock.CertainLockAccessibilityService
import com.example.certainlock.CertainLockForegroundService
import com.example.certainlock.CertainLockManager
import com.example.certainlock.InstalledAppItem
import com.example.data.SettingsRepository
import com.example.network.NetworkInfoManager
import com.example.network.NetworkStatus
import com.example.pin.PinManager
import com.example.pin.PinProtectionConfig
import com.example.schedule.ScheduleItem
import com.example.schedule.ScheduleManager
import com.example.timer.TimerManager
import com.example.timer.TimerState
import com.example.vpn.SultanBlockVpnService
import com.example.vpn.VpnStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecial: Boolean = false,
    val isAccessibility: Boolean = false,
    val isUsageAccess: Boolean = false
)

enum class ProtectedSection {
    INTERNET_BLOCK,
    VOLUME_LOCK,
    APP_LOCK_SETTINGS,
    APP_LOCK_SELECTION,
    SCHEDULE,
    TIMER,
    SECURITY_SETTINGS
}

data class PinChallenge(
    val section: ProtectedSection,
    val onAuthenticated: () -> Unit
)

class MainViewModel(
    val networkInfoManager: NetworkInfoManager,
    val settingsRepository: SettingsRepository,
    val protectionPreferencesRepository: com.example.data.ProtectionPreferencesRepository,
    val scheduleManager: ScheduleManager,
    val timerManager: TimerManager,
    val pinManager: PinManager
) : ViewModel() {

    // Theme Mode
    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.theme.AppThemeMode.SYSTEM)

    // Feature 1: Internet Block
    val isVpnActive: StateFlow<Boolean> = VpnStateManager.isVpnActive
    val blockedPackets: StateFlow<Long> = VpnStateManager.blockedPackets
    val blockedBytes: StateFlow<Long> = VpnStateManager.blockedBytes
    val vpnError: StateFlow<String?> = VpnStateManager.lastError

    // Feature 2: Volume Lock
    val isVolumeLockActive: StateFlow<Boolean> = VolumeStateManager.isLockActive
    val volumeSuppressionCount: StateFlow<Long> = VolumeStateManager.suppressionCount
    val lastSuppressedTime: StateFlow<Long?> = VolumeStateManager.lastSuppressedTime
    val audioStreams: StateFlow<List<AudioStreamInfo>> = VolumeStateManager.streams

    // Feature 3: Schedule
    val schedules: StateFlow<List<ScheduleItem>> = scheduleManager.schedules
    val nextActivationSummary: StateFlow<String> = scheduleManager.nextActivationSummary

    // Feature 4: Timer Mode
    val timerState: StateFlow<TimerState> = timerManager.timerState
    private val _timerCountdown = MutableStateFlow("00:00")
    val timerCountdown: StateFlow<String> = _timerCountdown.asStateFlow()

    // Feature 5: Certain Lock (App Lock)
    val isCertainLockActive: StateFlow<Boolean> = CertainLockManager.isLockActive
    val lockedPackages: StateFlow<Set<String>> = CertainLockManager.lockedPackages
    val installedApps: StateFlow<List<InstalledAppItem>> = CertainLockManager.installedApps
    val certainLockRedirectCount: StateFlow<Long> = CertainLockManager.redirectCount
    val lastRedirectedApp: StateFlow<String?> = CertainLockManager.lastRedirectedApp

    // Feature 6: PIN Lock
    val isPinSet: StateFlow<Boolean> = pinManager.isPinSet
    val pinConfig: StateFlow<PinProtectionConfig> = pinManager.config
    private val _activePinChallenge = MutableStateFlow<PinChallenge?>(null)
    val activePinChallenge: StateFlow<PinChallenge?> = _activePinChallenge.asStateFlow()
    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    // Telemetry & Permissions
    val networkStatus: StateFlow<NetworkStatus> = networkInfoManager.status
    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    val autoRestoreInternetOnBoot: StateFlow<Boolean> = settingsRepository.autoRestoreInternetOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoRestoreVolumeOnBoot: StateFlow<Boolean> = settingsRepository.autoRestoreVolumeOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lockMediaOnly: StateFlow<Boolean> = settingsRepository.lockMediaOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showCapabilityDialog = MutableStateFlow(false)
    val showCapabilityDialog: StateFlow<Boolean> = _showCapabilityDialog.asStateFlow()

    private val _showAddAppsDialog = MutableStateFlow(false)
    val showAddAppsDialog: StateFlow<Boolean> = _showAddAppsDialog.asStateFlow()

    private val _editingSchedule = MutableStateFlow<ScheduleItem?>(null)
    val editingSchedule: StateFlow<ScheduleItem?> = _editingSchedule.asStateFlow()
    private val _showScheduleDialog = MutableStateFlow(false)
    val showScheduleDialog: StateFlow<Boolean> = _showScheduleDialog.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        networkInfoManager.startListening()
        viewModelScope.launch {
            pinManager.initialize()
            scheduleManager.loadSchedules()
            timerManager.loadState()

            // Load saved CertainLock packages and toggle states from repository
            val persistedToggles = protectionPreferencesRepository.toggleStates.first()
            if (persistedToggles.lockedAppPackages.isNotEmpty()) {
                CertainLockManager.setLockedPackages(persistedToggles.lockedAppPackages)
            } else {
                val savedLocked = settingsRepository.lockedPackages.first()
                if (savedLocked.isNotEmpty()) {
                    CertainLockManager.setLockedPackages(savedLocked)
                    protectionPreferencesRepository.setLockedAppPackages(savedLocked)
                }
            }
            if (persistedToggles.isAppLockEnabled) {
                CertainLockManager.updateActiveState(true)
            } else {
                val savedCertainLockActive = settingsRepository.isCertainLockActive.first()
                CertainLockManager.updateActiveState(savedCertainLockActive)
            }

            // Start countdown ticker for timer
            while (true) {
                val state = timerManager.timerState.value
                if (state.isActive) {
                    _timerCountdown.value = state.formatRemaining()
                } else {
                    _timerCountdown.value = "00:00"
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkInfoManager.stopListening()
    }

    /**
     * Executes the target action only after PIN verification if protected.
     */
    fun performProtectedAction(section: ProtectedSection, action: () -> Unit) {
        val isPinActive = isPinSet.value
        val config = pinConfig.value
        val requiresPin = isPinActive && when (section) {
            ProtectedSection.INTERNET_BLOCK -> config.protectInternetBlock
            ProtectedSection.VOLUME_LOCK -> config.protectVolumeLock
            ProtectedSection.APP_LOCK_SETTINGS -> config.protectAppLockSettings
            ProtectedSection.APP_LOCK_SELECTION -> config.protectSelectedApps
            ProtectedSection.SCHEDULE -> config.protectSchedule
            ProtectedSection.TIMER -> config.protectTimer
            ProtectedSection.SECURITY_SETTINGS -> config.protectSecuritySettings
        }

        if (requiresPin && !pinManager.isSessionAuthenticated()) {
            _activePinChallenge.value = PinChallenge(section, action)
        } else {
            action()
        }
    }

    fun onPinChallengeResolved(success: Boolean) {
        val challenge = _activePinChallenge.value
        _activePinChallenge.value = null
        if (success && challenge != null) {
            challenge.onAuthenticated()
        }
    }

    fun lockSession() {
        pinManager.setSessionAuthenticated(false)
        setFeedback("Session locked. PIN authentication required.")
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.saveThemeMode(mode)
        }
    }

    fun checkAllPermissions(context: Context) {
        val vpnIntent = VpnService.prepare(context)

        val list = mutableListOf<PermissionItem>()

        list.add(
            PermissionItem(
                permission = "android.net.VpnService",
                title = "VPN Service Authorization",
                description = "Required to route all device traffic into loopback blackhole sink.",
                isGranted = vpnIntent == null,
                isSpecial = true
            )
        )

        // Accessibility Service check for Certain Lock
        val a11yGranted = CertainLockAccessibilityService.isServiceConnected
        list.add(
            PermissionItem(
                permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                title = "Certain Lock Accessibility",
                description = "Allows instant detection of restricted apps and immediate redirection to Home Screen.",
                isGranted = a11yGranted,
                isAccessibility = true
            )
        )

        // Usage Access check
        val hasUsageAccess = checkUsageAccessGranted(context)
        list.add(
            PermissionItem(
                permission = "android.permission.PACKAGE_USAGE_STATS",
                title = "Usage Access (Fallback)",
                description = "Companion foreground app detection for Certain Lock when Accessibility is disabled.",
                isGranted = hasUsageAccess,
                isUsageAccess = true
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotification = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            list.add(
                PermissionItem(
                    permission = android.Manifest.permission.POST_NOTIFICATIONS,
                    title = "Foreground Notification",
                    description = "Required by Android 13+ to display ongoing status and 1-tap recovery controls.",
                    isGranted = hasNotification
                )
            )
        }

        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                permission = android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                title = "Modify Audio Settings",
                description = "Enables programmatic volume adjustment and stream muting.",
                isGranted = hasAudio
            )
        )

        val hasNetwork = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                permission = android.Manifest.permission.ACCESS_NETWORK_STATE,
                title = "Access Network State",
                description = "Allows live detection of Wi-Fi, Cellular, and VPN routing capabilities.",
                isGranted = hasNetwork
            )
        )

        _permissions.value = list
    }

    private fun checkUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun startInternetBlock(context: Context) {
        performProtectedAction(ProtectedSection.INTERNET_BLOCK) {
            val intent = Intent(context, SultanBlockVpnService::class.java).apply {
                action = SultanBlockVpnService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            viewModelScope.launch {
                protectionPreferencesRepository.setInternetBlockEnabled(true)
            }
            setFeedback("Activating internet blackhole tunnel...")
        }
    }

    fun stopInternetBlock(context: Context) {
        performProtectedAction(ProtectedSection.INTERNET_BLOCK) {
            val intent = Intent(context, SultanBlockVpnService::class.java).apply {
                action = SultanBlockVpnService.ACTION_STOP
            }
            context.startService(intent)
            viewModelScope.launch {
                protectionPreferencesRepository.setInternetBlockEnabled(false)
            }
            setFeedback("Internet traffic restored to normal.")
        }
    }

    fun toggleVolumeLock(context: Context) {
        performProtectedAction(ProtectedSection.VOLUME_LOCK) {
            val active = isVolumeLockActive.value
            val newActive = !active
            val intent = Intent(context, VolumeLockService::class.java).apply {
                action = if (active) VolumeLockService.ACTION_STOP else VolumeLockService.ACTION_START
            }
            if (newActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                setFeedback("Volume lock engaged. Clamping all audio streams to 0.")
            } else {
                context.startService(intent)
                setFeedback("Volume lock disengaged. Normal audio restored.")
            }
            viewModelScope.launch {
                protectionPreferencesRepository.setVolumeLockEnabled(newActive)
            }
        }
    }

    fun toggleCertainLock(context: Context) {
        performProtectedAction(ProtectedSection.APP_LOCK_SETTINGS) {
            val active = isCertainLockActive.value
            val newActive = !active
            CertainLockManager.updateActiveState(newActive)
            viewModelScope.launch {
                settingsRepository.saveCertainLockActive(newActive)
                protectionPreferencesRepository.setAppLockEnabled(newActive)
            }

            val intent = Intent(context, CertainLockForegroundService::class.java).apply {
                action = if (newActive) CertainLockForegroundService.ACTION_START else CertainLockForegroundService.ACTION_STOP
            }
            if (newActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                setFeedback("Certain Lock enabled. Restricted apps will be returned to Home.")
            } else {
                context.startService(intent)
                setFeedback("Certain Lock disabled.")
            }
        }
    }

    fun loadApps(context: Context) {
        viewModelScope.launch {
            CertainLockManager.loadInstalledApps(context)
        }
    }

    fun toggleAppLocked(packageName: String) {
        performProtectedAction(ProtectedSection.APP_LOCK_SELECTION) {
            val updated = CertainLockManager.toggleAppLocked(packageName)
            viewModelScope.launch {
                settingsRepository.saveLockedPackages(updated)
                protectionPreferencesRepository.setLockedAppPackages(updated)
            }
        }
    }

    fun removeLockedApp(packageName: String) {
        performProtectedAction(ProtectedSection.APP_LOCK_SELECTION) {
            val updated = CertainLockManager.removeLockedApp(packageName)
            viewModelScope.launch {
                settingsRepository.saveLockedPackages(updated)
                protectionPreferencesRepository.setLockedAppPackages(updated)
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            setFeedback("Could not open Accessibility settings: ${e.message}")
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            setFeedback("Could not open Usage Access settings: ${e.message}")
        }
    }

    fun openScheduleDialog(item: ScheduleItem? = null) {
        performProtectedAction(ProtectedSection.SCHEDULE) {
            _editingSchedule.value = item
            _showScheduleDialog.value = true
        }
    }

    fun dismissScheduleDialog() {
        _showScheduleDialog.value = false
        _editingSchedule.value = null
    }

    fun saveSchedule(item: ScheduleItem) {
        viewModelScope.launch {
            scheduleManager.addOrUpdateSchedule(item)
            dismissScheduleDialog()
            setFeedback("Schedule '${item.title}' saved.")
        }
    }

    fun deleteSchedule(id: String) {
        performProtectedAction(ProtectedSection.SCHEDULE) {
            viewModelScope.launch {
                scheduleManager.deleteSchedule(id)
                setFeedback("Schedule removed.")
            }
        }
    }

    fun toggleSchedule(id: String) {
        performProtectedAction(ProtectedSection.SCHEDULE) {
            viewModelScope.launch {
                scheduleManager.toggleScheduleEnabled(id)
            }
        }
    }

    fun startTimer(durationMinutes: Long, ctrlInternet: Boolean, ctrlVolume: Boolean, ctrlAppLock: Boolean) {
        performProtectedAction(ProtectedSection.TIMER) {
            viewModelScope.launch {
                timerManager.startTimer(durationMinutes, ctrlInternet, ctrlVolume, ctrlAppLock)
                setFeedback("Timer started for $durationMinutes minutes.")
            }
        }
    }

    fun cancelTimer() {
        performProtectedAction(ProtectedSection.TIMER) {
            viewModelScope.launch {
                timerManager.cancelTimer()
                setFeedback("Timer cancelled.")
            }
        }
    }

    fun emergencyRestoreAll(context: Context) {
        performProtectedAction(ProtectedSection.SECURITY_SETTINGS) {
            stopInternetBlock(context)
            val volIntent = Intent(context, VolumeLockService::class.java).apply {
                action = VolumeLockService.ACTION_STOP
            }
            context.startService(volIntent)

            CertainLockManager.updateActiveState(false)
            val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                action = CertainLockForegroundService.ACTION_STOP
            }
            context.startService(clIntent)

            viewModelScope.launch {
                timerManager.cancelTimer()
                protectionPreferencesRepository.clearAllToggles()
            }
            setFeedback("Emergency Disarm: All protections disabled.")
        }
    }

    fun openSystemWifiSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent("android.settings.panel.action.INTERNET").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun runConnectivityProbe() {
        viewModelScope.launch {
            networkInfoManager.performRealConnectivityProbe()
        }
    }

    fun setAutoRestoreInternet(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRestoreInternetOnBoot(enabled)
        }
    }

    fun setAutoRestoreVolume(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRestoreVolumeOnBoot(enabled)
        }
    }

    fun setLockMediaOnly(mediaOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLockMediaOnly(mediaOnly)
            protectionPreferencesRepository.setLockMediaOnly(mediaOnly)
            VolumeStateManager.setLockMediaOnly(mediaOnly)
        }
    }

    fun updatePinConfig(newConfig: PinProtectionConfig) {
        performProtectedAction(ProtectedSection.SECURITY_SETTINGS) {
            viewModelScope.launch {
                pinManager.updateConfig(newConfig)
                setFeedback("PIN protection settings updated.")
            }
        }
    }

    fun setShowCapabilityDialog(show: Boolean) {
        _showCapabilityDialog.value = show
    }

    fun setShowAddAppsDialog(show: Boolean) {
        _showAddAppsDialog.value = show
    }

    fun setShowPinSetupDialog(show: Boolean) {
        _showPinSetupDialog.value = show
    }

    fun setFeedback(msg: String?) {
        _feedbackMessage.value = msg
    }
}
