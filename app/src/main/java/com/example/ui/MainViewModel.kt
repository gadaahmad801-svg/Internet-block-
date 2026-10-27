package com.example.ui

import android.app.Activity
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
import com.example.data.SettingsRepository
import com.example.network.NetworkInfoManager
import com.example.network.NetworkStatus
import com.example.vpn.SultanBlockVpnService
import com.example.vpn.VpnStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecial: Boolean = false
)

class MainViewModel(
    private val networkInfoManager: NetworkInfoManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isVpnActive: StateFlow<Boolean> = VpnStateManager.isVpnActive
    val blockedPackets: StateFlow<Long> = VpnStateManager.blockedPackets
    val blockedBytes: StateFlow<Long> = VpnStateManager.blockedBytes
    val vpnError: StateFlow<String?> = VpnStateManager.lastError

    val isVolumeLockActive: StateFlow<Boolean> = VolumeStateManager.isLockActive
    val volumeSuppressionCount: StateFlow<Long> = VolumeStateManager.suppressionCount
    val lastSuppressedTime: StateFlow<Long?> = VolumeStateManager.lastSuppressedTime
    val audioStreams: StateFlow<List<AudioStreamInfo>> = VolumeStateManager.streams

    val networkStatus: StateFlow<NetworkStatus> = networkInfoManager.status

    val autoRestoreInternetOnBoot: StateFlow<Boolean> = settingsRepository.autoRestoreInternetOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoRestoreVolumeOnBoot: StateFlow<Boolean> = settingsRepository.autoRestoreVolumeOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockMediaOnly: StateFlow<Boolean> = settingsRepository.lockMediaOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    private val _isVpnPrepared = MutableStateFlow(false)
    val isVpnPrepared: StateFlow<Boolean> = _isVpnPrepared.asStateFlow()

    private val _showCapabilityDialog = MutableStateFlow(false)
    val showCapabilityDialog: StateFlow<Boolean> = _showCapabilityDialog.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        networkInfoManager.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        networkInfoManager.stopListening()
    }

    fun checkAllPermissions(context: Context) {
        val vpnIntent = VpnService.prepare(context)
        _isVpnPrepared.value = (vpnIntent == null)

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

        val hasWifiState = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                permission = android.Manifest.permission.ACCESS_WIFI_STATE,
                title = "Access Wi-Fi State",
                description = "Allows monitoring Wi-Fi radio status, SSID, and connection link metrics.",
                isGranted = hasWifiState
            )
        )

        _permissions.value = list
    }

    fun startInternetBlock(context: Context) {
        val intent = Intent(context, SultanBlockVpnService::class.java).apply {
            action = SultanBlockVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        setFeedback("Activating internet blackhole tunnel...")
    }

    fun stopInternetBlock(context: Context) {
        val intent = Intent(context, SultanBlockVpnService::class.java).apply {
            action = SultanBlockVpnService.ACTION_STOP
        }
        context.startService(intent)
        setFeedback("Internet traffic restored to normal.")
    }

    fun toggleVolumeLock(context: Context) {
        val active = isVolumeLockActive.value
        val intent = Intent(context, VolumeLockService::class.java).apply {
            action = if (active) VolumeLockService.ACTION_STOP else VolumeLockService.ACTION_START
        }
        if (!active) {
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
    }

    fun runConnectivityProbe() {
        viewModelScope.launch {
            networkInfoManager.performRealConnectivityProbe()
        }
    }

    fun emergencyRestoreAll(context: Context) {
        stopInternetBlock(context)
        val intent = Intent(context, VolumeLockService::class.java).apply {
            action = VolumeLockService.ACTION_STOP
        }
        context.startService(intent)
        setFeedback("Emergency Disarm: Internet restored & Volume unlocked.")
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
            VolumeStateManager.setLockMediaOnly(mediaOnly)
        }
    }

    fun setShowCapabilityDialog(show: Boolean) {
        _showCapabilityDialog.value = show
    }

    fun setFeedback(msg: String?) {
        _feedbackMessage.value = msg
    }
}
