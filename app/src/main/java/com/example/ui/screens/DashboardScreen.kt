package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.certainlock.AddAppsDialog
import com.example.certainlock.InstalledAppItem
import com.example.network.NetworkStatus
import com.example.pin.PinAuthDialog
import com.example.pin.PinProtectionConfig
import com.example.pin.PinSetupDialog
import com.example.schedule.ScheduleDialog
import com.example.schedule.ScheduleItem
import com.example.timer.TimerState
import com.example.ui.MainViewModel
import com.example.ui.PermissionItem
import com.example.ui.ProtectedSection
import com.example.ui.components.CapabilityReportDialog
import com.example.ui.components.CyberCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.SultanAmber
import com.example.ui.theme.SultanCardBorder
import com.example.ui.theme.SultanCyan
import com.example.ui.theme.SultanDarkBg
import com.example.ui.theme.SultanEmerald
import com.example.ui.theme.SultanRed
import com.example.ui.theme.SultanSurface
import com.example.ui.theme.SultanSurfaceCard
import com.example.ui.theme.SultanSurfaceElevated
import com.example.ui.theme.SultanTextMuted
import com.example.ui.theme.SultanTextPrimary
import com.example.ui.theme.SultanTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State bindings
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val blockedPackets by viewModel.blockedPackets.collectAsState()
    val blockedBytes by viewModel.blockedBytes.collectAsState()
    val vpnError by viewModel.vpnError.collectAsState()

    val isVolumeLockActive by viewModel.isVolumeLockActive.collectAsState()
    val volumeSuppressionCount by viewModel.volumeSuppressionCount.collectAsState()
    val audioStreams by viewModel.audioStreams.collectAsState()

    val schedules by viewModel.schedules.collectAsState()
    val nextActivationSummary by viewModel.nextActivationSummary.collectAsState()

    val timerState by viewModel.timerState.collectAsState()
    val timerCountdown by viewModel.timerCountdown.collectAsState()

    val isCertainLockActive by viewModel.isCertainLockActive.collectAsState()
    val lockedPackages by viewModel.lockedPackages.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val certainLockRedirectCount by viewModel.certainLockRedirectCount.collectAsState()

    val isPinSet by viewModel.isPinSet.collectAsState()
    val pinConfig by viewModel.pinConfig.collectAsState()
    val activePinChallenge by viewModel.activePinChallenge.collectAsState()
    val showPinSetupDialog by viewModel.showPinSetupDialog.collectAsState()

    val networkStatus by viewModel.networkStatus.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    val autoRestoreInternet by viewModel.autoRestoreInternetOnBoot.collectAsState()
    val autoRestoreVolume by viewModel.autoRestoreVolumeOnBoot.collectAsState()
    val lockMediaOnly by viewModel.lockMediaOnly.collectAsState()

    val showCapabilityDialog by viewModel.showCapabilityDialog.collectAsState()
    val showAddAppsDialog by viewModel.showAddAppsDialog.collectAsState()
    val showScheduleDialog by viewModel.showScheduleDialog.collectAsState()
    val editingSchedule by viewModel.editingSchedule.collectAsState()

    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Activity result launcher for VPN consent dialog
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startInternetBlock(context)
            viewModel.checkAllPermissions(context)
        } else {
            viewModel.setFeedback("VPN authorization was declined by user.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkAllPermissions(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkAllPermissions(context)
        viewModel.loadApps(context)
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.setFeedback(null)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        containerColor = SultanDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Emergency Disarm
            item {
                HeaderSection(
                    isVpnActive = isVpnActive,
                    isVolumeLockActive = isVolumeLockActive,
                    isCertainLockActive = isCertainLockActive,
                    isTimerActive = timerState.isActive,
                    onEmergencyRestore = { viewModel.emergencyRestoreAll(context) }
                )
            }

            // Error banner if any
            if (vpnError != null) {
                item {
                    ErrorBanner(
                        message = vpnError!!,
                        onDismiss = { viewModel.setFeedback(null) }
                    )
                }
            }

            // CARD 1: INTERNET BLOCK
            item {
                InternetBlockCard(
                    isVpnActive = isVpnActive,
                    blockedPackets = blockedPackets,
                    blockedBytes = blockedBytes,
                    onToggle = { enable ->
                        if (enable) {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnLauncher.launch(prepareIntent)
                            } else {
                                viewModel.startInternetBlock(context)
                            }
                        } else {
                            viewModel.stopInternetBlock(context)
                        }
                    }
                )
            }

            // CARD 2: VOLUME LOCK
            item {
                VolumeLockCard(
                    isVolumeLockActive = isVolumeLockActive,
                    volumeSuppressionCount = volumeSuppressionCount,
                    audioStreams = audioStreams,
                    lockMediaOnly = lockMediaOnly,
                    onToggleLock = { viewModel.toggleVolumeLock(context) },
                    onToggleMediaOnly = { viewModel.setLockMediaOnly(it) }
                )
            }

            // CARD 3: SCHEDULE
            item {
                ScheduleCard(
                    schedules = schedules,
                    nextActivationSummary = nextActivationSummary,
                    onAddSchedule = { viewModel.openScheduleDialog(null) },
                    onEditSchedule = { viewModel.openScheduleDialog(it) },
                    onDeleteSchedule = { viewModel.deleteSchedule(it.id) },
                    onToggleSchedule = { viewModel.toggleSchedule(it.id) }
                )
            }

            // CARD 4: TIMER MODE
            item {
                TimerCard(
                    timerState = timerState,
                    countdownText = timerCountdown,
                    onStartTimer = { duration, net, vol, appLock ->
                        viewModel.startTimer(duration, net, vol, appLock)
                    },
                    onCancelTimer = { viewModel.cancelTimer() }
                )
            }

            // CARD 5: APP LOCK — CERTAIN LOCK
            item {
                CertainLockCard(
                    isLockActive = isCertainLockActive,
                    lockedPackages = lockedPackages,
                    installedApps = installedApps,
                    redirectCount = certainLockRedirectCount,
                    onToggleMaster = { viewModel.toggleCertainLock(context) },
                    onOpenAddApps = { viewModel.setShowAddAppsDialog(true) },
                    onRemoveApp = { viewModel.removeLockedApp(it) },
                    onOpenAccessibility = { viewModel.openAccessibilitySettings(context) }
                )
            }

            // CARD 6: PIN LOCK
            item {
                PinLockCard(
                    isPinSet = isPinSet,
                    config = pinConfig,
                    onConfigurePin = { viewModel.setShowPinSetupDialog(true) },
                    onUpdateConfig = { newConfig ->
                        viewModel.updatePinConfig(newConfig)
                    }
                )
            }

            // CARD 7: SYSTEM STATUS & REAL PROBE
            item {
                SystemStatusCard(
                    networkStatus = networkStatus,
                    isVpnActive = isVpnActive,
                    onProbe = { viewModel.runConnectivityProbe() },
                    onOpenWifiPanel = { viewModel.openSystemWifiSettings(context) }
                )
            }

            // CARD 8: PERMISSIONS & AUTHORIZATIONS
            item {
                PermissionsCard(
                    permissions = permissions,
                    onRequestPermission = { item ->
                        when {
                            item.permission == "android.net.VpnService" -> {
                                val prep = VpnService.prepare(context)
                                if (prep != null) vpnLauncher.launch(prep)
                            }
                            item.isAccessibility -> {
                                viewModel.openAccessibilitySettings(context)
                            }
                            item.isUsageAccess -> {
                                viewModel.openUsageAccessSettings(context)
                            }
                            else -> {
                                permissionLauncher.launch(item.permission)
                            }
                        }
                    }
                )
            }

            // CARD 9: SETTINGS & CAPABILITY REPORT
            item {
                SettingsCard(
                    autoRestoreInternet = autoRestoreInternet,
                    autoRestoreVolume = autoRestoreVolume,
                    onToggleRestoreInternet = { viewModel.setAutoRestoreInternet(it) },
                    onToggleRestoreVolume = { viewModel.setAutoRestoreVolume(it) },
                    onOpenCapabilityReport = { viewModel.setShowCapabilityDialog(true) }
                )
            }
        }
    }

    // PIN Authentication Dialog Guard
    if (activePinChallenge != null) {
        PinAuthDialog(
            pinManager = viewModel.pinManager,
            onSuccess = { viewModel.onPinChallengeResolved(true) },
            onDismiss = { viewModel.onPinChallengeResolved(false) }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            pinManager = viewModel.pinManager,
            onComplete = {
                viewModel.setShowPinSetupDialog(false)
                viewModel.setFeedback("PIN lock active.")
            },
            onDismiss = { viewModel.setShowPinSetupDialog(false) }
        )
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        ScheduleDialog(
            initialItem = editingSchedule,
            onSave = { viewModel.saveSchedule(it) },
            onDismiss = { viewModel.dismissScheduleDialog() }
        )
    }

    // Add Apps Dialog (Certain Lock)
    if (showAddAppsDialog) {
        AddAppsDialog(
            installedApps = installedApps,
            onToggleApp = { viewModel.toggleAppLocked(it) },
            onDismiss = { viewModel.setShowAddAppsDialog(false) }
        )
    }

    // Capability Report Dialog
    if (showCapabilityDialog) {
        CapabilityReportDialog(onDismiss = { viewModel.setShowCapabilityDialog(false) })
    }
}

@Composable
private fun HeaderSection(
    isVpnActive: Boolean,
    isVolumeLockActive: Boolean,
    isCertainLockActive: Boolean,
    isTimerActive: Boolean,
    onEmergencyRestore: () -> Unit
) {
    val anyActive = isVpnActive || isVolumeLockActive || isCertainLockActive || isTimerActive

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (anyActive) SultanRed else SultanEmerald)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SULTAN BLOCK",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = SultanTextPrimary
                )
            }
            Text(
                text = "NATIVE SYSTEM CONTROL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = SultanTextSecondary
            )
        }

        if (anyActive) {
            Button(
                onClick = onEmergencyRestore,
                colors = ButtonDefaults.buttonColors(containerColor = SultanRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("emergency_restore_button")
            ) {
                Text(
                    text = "DISARM ALL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SultanRed
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SultanRed.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SultanRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SultanRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = SultanTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                "DISMISS",
                color = SultanRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    }
}

@Composable
private fun InternetBlockCard(
    isVpnActive: Boolean,
    blockedPackets: Long,
    blockedBytes: Long,
    onToggle: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isVpnActive) SultanRed else SultanCardBorder,
        label = "vpnBorder"
    )

    CyberCard(
        title = "INTERNET BLOCK",
        subtitle = "Blackholes all device IPv4, IPv6 & DNS traffic",
        icon = if (isVpnActive) Icons.Default.Block else Icons.Default.Security,
        iconTint = if (isVpnActive) SultanRed else SultanEmerald,
        borderColor = borderColor,
        trailingBadge = {
            StatusPill(
                text = if (isVpnActive) "BLOCKED" else "ONLINE",
                color = if (isVpnActive) SultanRed else SultanEmerald
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MASTER INTERNET BLOCK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                Text(
                    text = if (isVpnActive) "Loopback active. Packets dropped immediately." else "Internet traffic is flowing normally.",
                    fontSize = 11.sp,
                    color = SultanTextSecondary
                )
            }

            Switch(
                checked = isVpnActive,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("internet_block_toggle"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SultanRed,
                    uncheckedThumbColor = SultanTextMuted,
                    uncheckedTrackColor = SultanSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "PACKETS SUNK",
                value = String.format("%,d", blockedPackets),
                color = if (isVpnActive) SultanRed else SultanTextSecondary,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "DATA INTERCEPTED",
                value = formatBytes(blockedBytes),
                color = if (isVpnActive) SultanRed else SultanTextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VolumeLockCard(
    isVolumeLockActive: Boolean,
    volumeSuppressionCount: Long,
    audioStreams: List<com.example.audio.AudioStreamInfo>,
    lockMediaOnly: Boolean,
    onToggleLock: () -> Unit,
    onToggleMediaOnly: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isVolumeLockActive) SultanRed else SultanCardBorder,
        label = "volumeBorder"
    )

    CyberCard(
        title = "VOLUME LOCK",
        subtitle = "Continuous audio mute & stream clamp",
        icon = if (isVolumeLockActive) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
        iconTint = if (isVolumeLockActive) SultanRed else SultanEmerald,
        borderColor = borderColor,
        trailingBadge = {
            StatusPill(
                text = if (isVolumeLockActive) "LOCKED (0dB)" else "NORMAL",
                color = if (isVolumeLockActive) SultanRed else SultanEmerald
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MASTER VOLUME LOCK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                Text(
                    text = if (isVolumeLockActive) "Actively clamping audio streams to zero." else "Normal audio output permitted.",
                    fontSize = 11.sp,
                    color = SultanTextSecondary
                )
            }

            Switch(
                checked = isVolumeLockActive,
                onCheckedChange = { onToggleLock() },
                modifier = Modifier.testTag("volume_lock_toggle"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SultanRed,
                    uncheckedThumbColor = SultanTextMuted,
                    uncheckedTrackColor = SultanSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "SUPPRESSION HITS",
                value = "$volumeSuppressionCount times",
                color = if (isVolumeLockActive) SultanRed else SultanTextSecondary,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "ENFORCEMENT ENGINE",
                value = "Observer + Focus",
                color = SultanCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedules: List<ScheduleItem>,
    nextActivationSummary: String,
    onAddSchedule: () -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    onDeleteSchedule: (ScheduleItem) -> Unit,
    onToggleSchedule: (ScheduleItem) -> Unit
) {
    CyberCard(
        title = "SCHEDULE",
        subtitle = "Automated protection windows",
        icon = Icons.Default.Schedule,
        iconTint = SultanCyan,
        trailingBadge = {
            Button(
                onClick = onAddSchedule,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_schedule_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanCyan)
            }
        }
    ) {
        // Next Activation Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SultanDarkBg)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("NEXT ACTIVATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
                Text(nextActivationSummary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SultanTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (schedules.isEmpty()) {
            Text("No schedules created. Tap '+ ADD' above to configure automated focus windows.", fontSize = 11.sp, color = SultanTextMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                schedules.forEach { item ->
                    ScheduleRow(
                        item = item,
                        onEdit = { onEditSchedule(item) },
                        onDelete = { onDeleteSchedule(item) },
                        onToggle = { onToggleSchedule(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SultanDarkBg)
            .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
            Text(
                text = "${item.formatStartTime()} - ${item.formatEndTime()} • ${item.formatDays()}",
                fontSize = 11.sp,
                color = SultanCyan
            )
            val controlled = mutableListOf<String>().apply {
                if (item.controlInternet) add("Internet")
                if (item.controlVolume) add("Volume")
                if (item.controlAppLock) add("CertainLock")
            }.joinToString(", ")
            Text("Controls: $controlled", fontSize = 10.sp, color = SultanTextMuted)
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SultanTextSecondary, modifier = Modifier.size(16.dp))
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SultanRed, modifier = Modifier.size(16.dp))
        }

        Switch(
            checked = item.isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SultanCyan,
                uncheckedThumbColor = SultanTextMuted,
                uncheckedTrackColor = SultanSurfaceElevated
            )
        )
    }
}

@Composable
private fun TimerCard(
    timerState: TimerState,
    countdownText: String,
    onStartTimer: (Long, Boolean, Boolean, Boolean) -> Unit,
    onCancelTimer: () -> Unit
) {
    var selectedMinutes by remember { mutableLongStateOf(60L) }
    var ctrlNet by remember { mutableStateOf(true) }
    var ctrlVol by remember { mutableStateOf(true) }
    var ctrlAppLock by remember { mutableStateOf(true) }

    CyberCard(
        title = "TIMER MODE",
        subtitle = "Quick temporary countdown protection",
        icon = if (timerState.isActive) Icons.Default.HourglassBottom else Icons.Default.HourglassTop,
        iconTint = if (timerState.isActive) SultanRed else SultanCyan,
        trailingBadge = {
            StatusPill(
                text = if (timerState.isActive) "RUNNING" else "STANDBY",
                color = if (timerState.isActive) SultanRed else SultanTextMuted
            )
        }
    ) {
        if (timerState.isActive) {
            // Active countdown display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SultanDarkBg)
                    .border(1.dp, SultanRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TIME REMAINING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = countdownText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = SultanRed
                )
                Spacer(modifier = Modifier.height(8.dp))

                val protectedFeatures = mutableListOf<String>().apply {
                    if (timerState.controlInternet) add("Internet Block")
                    if (timerState.controlVolume) add("Volume Lock")
                    if (timerState.controlAppLock) add("Certain Lock")
                }.joinToString(" • ")

                Text(protectedFeatures, fontSize = 11.sp, color = SultanTextSecondary)

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCancelTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = SultanRed.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SultanRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("cancel_timer_button")
                ) {
                    Text("CANCEL TIMER", color = SultanRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        } else {
            // Preset duration chips
            Text("SELECT DURATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
            Spacer(modifier = Modifier.height(6.dp))

            val presets = listOf(30L to "30 Min", 60L to "1 Hour", 120L to "2 Hours", 180L to "3 Hours")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (mins, label) ->
                    val isSelected = selectedMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SultanCyan else SultanDarkBg)
                            .border(1.dp, if (isSelected) SultanCyan else SultanCardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedMinutes = mins }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else SultanTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Protections checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimerFeatureCheck("Internet", ctrlNet) { ctrlNet = it }
                TimerFeatureCheck("Volume", ctrlVol) { ctrlVol = it }
                TimerFeatureCheck("Certain Lock", ctrlAppLock) { ctrlAppLock = it }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onStartTimer(selectedMinutes, ctrlNet, ctrlVol, ctrlAppLock) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_timer_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan)
            ) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("START TIMER ($selectedMinutes MIN)", color = SultanCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimerFeatureCheck(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onToggle(!checked) }
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = SultanCyan,
                uncheckedColor = SultanCardBorder
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = SultanTextPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CertainLockCard(
    isLockActive: Boolean,
    lockedPackages: Set<String>,
    installedApps: List<InstalledAppItem>,
    redirectCount: Long,
    onToggleMaster: () -> Unit,
    onOpenAddApps: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isLockActive) SultanRed else SultanCardBorder,
        label = "certainLockBorder"
    )

    CyberCard(
        title = "APP LOCK — CERTAIN LOCK",
        subtitle = "Returns restricted apps to Home Screen upon launch",
        icon = Icons.Default.Apps,
        iconTint = if (isLockActive) SultanRed else SultanEmerald,
        borderColor = borderColor,
        trailingBadge = {
            StatusPill(
                text = if (isLockActive) "ACTIVE" else "DISABLED",
                color = if (isLockActive) SultanRed else SultanTextMuted
            )
        }
    ) {
        // Master switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MASTER CERTAIN LOCK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                Text(
                    text = if (isLockActive) "Actively redirecting restricted apps to Home Screen." else "App restrictions disarmed.",
                    fontSize = 11.sp,
                    color = SultanTextSecondary
                )
            }

            Switch(
                checked = isLockActive,
                onCheckedChange = { onToggleMaster() },
                modifier = Modifier.testTag("certain_lock_master_toggle"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SultanRed,
                    uncheckedThumbColor = SultanTextMuted,
                    uncheckedTrackColor = SultanSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Apps row & Add Apps button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${lockedPackages.size} RESTRICTED APPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SultanTextMuted
            )

            Button(
                onClick = onOpenAddApps,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanRed),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_apps_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = SultanRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD APPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanRed)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Flow of selected app chips
        if (lockedPackages.isEmpty()) {
            Text("No applications selected. Tap '+ ADD APPS' to select Instagram, ChatGPT, BGMI, etc.", fontSize = 11.sp, color = SultanTextMuted)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                lockedPackages.forEach { pkg ->
                    val appName = installedApps.find { it.packageName == pkg }?.appName ?: pkg.substringAfterLast('.')
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SultanDarkBg)
                            .border(1.dp, SultanCardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(appName, fontSize = 11.sp, color = SultanTextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = SultanRed,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRemoveApp(pkg) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        MetricTile(
            label = "REDIRECTED TO HOME",
            value = "$redirectCount times",
            color = if (isLockActive) SultanRed else SultanTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PinLockCard(
    isPinSet: Boolean,
    config: PinProtectionConfig,
    onConfigurePin: () -> Unit,
    onUpdateConfig: (PinProtectionConfig) -> Unit
) {
    CyberCard(
        title = "PIN LOCK",
        subtitle = "Salted cryptographic protection for settings & toggles",
        icon = Icons.Default.Lock,
        iconTint = if (isPinSet) SultanEmerald else SultanAmber,
        trailingBadge = {
            StatusPill(
                text = if (isPinSet) "ARMED" else "NO PIN",
                color = if (isPinSet) SultanEmerald else SultanAmber
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SultanDarkBg)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPinSet) "PIN IS ACTIVE" else "NO PIN CONFIGURED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SultanTextPrimary
                )
                Text(
                    text = if (isPinSet) "Protected sections require PIN to modify." else "Create a PIN to prevent unauthorized disarming.",
                    fontSize = 10.sp,
                    color = SultanTextMuted
                )
            }

            Button(
                onClick = onConfigurePin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("configure_pin_button")
            ) {
                Text(if (isPinSet) "CHANGE PIN" else "CREATE PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanCyan)
            }
        }

        if (isPinSet) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("PROTECTED SECTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PinToggleRow("Protect Internet Block Toggle", config.protectInternetBlock) {
                    onUpdateConfig(config.copy(protectInternetBlock = it))
                }
                PinToggleRow("Protect Volume Lock Toggle", config.protectVolumeLock) {
                    onUpdateConfig(config.copy(protectVolumeLock = it))
                }
                PinToggleRow("Protect Certain Lock Settings", config.protectCertainLock) {
                    onUpdateConfig(config.copy(protectCertainLock = it))
                }
                PinToggleRow("Protect Schedule Settings", config.protectSchedule) {
                    onUpdateConfig(config.copy(protectSchedule = it))
                }
                PinToggleRow("Protect Timer Settings", config.protectTimer) {
                    onUpdateConfig(config.copy(protectTimer = it))
                }
            }
        }
    }
}

@Composable
private fun PinToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = SultanTextSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SultanEmerald,
                uncheckedThumbColor = SultanTextMuted,
                uncheckedTrackColor = SultanSurfaceElevated
            )
        )
    }
}

@Composable
private fun SystemStatusCard(
    networkStatus: NetworkStatus,
    isVpnActive: Boolean,
    onProbe: () -> Unit,
    onOpenWifiPanel: () -> Unit
) {
    CyberCard(
        title = "SYSTEM STATUS",
        subtitle = "Live routing telemetry & real probe verification",
        icon = Icons.Default.NetworkCheck,
        iconTint = SultanCyan,
        trailingBadge = {
            StatusPill(
                text = if (isVpnActive) "SECURE / SINK" else "UNLOCKED",
                color = if (isVpnActive) SultanRed else SultanEmerald
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .padding(14.dp)
        ) {
            StatusRow("Active Transport", networkStatus.activeTransportName)
            StatusRow("Wi-Fi Interface", if (networkStatus.isWifiConnected) "Connected" else "Disconnected")
            StatusRow("Cellular Mobile", if (networkStatus.isCellularConnected) "Available" else "Inactive")
            StatusRow("VPN Tunnel", if (isVpnActive) "Active (Dropping 100% Packets)" else "Inactive")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = SultanCardBorder.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("REAL-TIME LEAKAGE PROBE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                    Text(
                        text = networkStatus.lastProbeResult,
                        fontSize = 11.sp,
                        color = if (networkStatus.isInternetReachable) SultanEmerald else SultanRed
                    )
                }

                Button(
                    onClick = onProbe,
                    enabled = !networkStatus.isProbing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("verify_leakage_probe_button")
                ) {
                    if (networkStatus.isProbing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SultanCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onOpenWifiPanel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanCardBorder)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LAUNCH WI-FI PANEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    permissions: List<PermissionItem>,
    onRequestPermission: (PermissionItem) -> Unit
) {
    CyberCard(
        title = "PERMISSIONS & AUTHORIZATIONS",
        subtitle = "Android security boundary assessment",
        icon = Icons.Default.VpnKey,
        iconTint = SultanAmber,
        trailingBadge = {
            val allGranted = permissions.all { it.isGranted }
            StatusPill(
                text = if (allGranted) "ALL GRANTED" else "PENDING",
                color = if (allGranted) SultanEmerald else SultanAmber
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            permissions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SultanDarkBg)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (item.isGranted) SultanEmerald else SultanAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                        Text(item.description, fontSize = 10.sp, color = SultanTextSecondary)
                    }
                    if (!item.isGranted) {
                        Button(
                            onClick = { onRequestPermission(item) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SultanAmber.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SultanAmber),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("grant_permission_${item.title.lowercase().replace(' ', '_')}")
                        ) {
                            Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanAmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    autoRestoreInternet: Boolean,
    autoRestoreVolume: Boolean,
    onToggleRestoreInternet: (Boolean) -> Unit,
    onToggleRestoreVolume: (Boolean) -> Unit,
    onOpenCapabilityReport: () -> Unit
) {
    CyberCard(
        title = "SETTINGS & ARCHITECTURE",
        subtitle = "Persistence preferences & platform report",
        icon = Icons.Default.Settings,
        iconTint = SultanTextSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Restore Internet Block on Boot", fontSize = 12.sp, color = SultanTextPrimary)
                    Text("Re-arms blackhole sinkhole upon system startup", fontSize = 10.sp, color = SultanTextMuted)
                }
                Switch(
                    checked = autoRestoreInternet,
                    onCheckedChange = onToggleRestoreInternet,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SultanRed,
                        uncheckedThumbColor = SultanTextMuted,
                        uncheckedTrackColor = SultanSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Restore Volume Lock on Boot", fontSize = 12.sp, color = SultanTextPrimary)
                    Text("Re-engages audio mute clamp after reboot", fontSize = 10.sp, color = SultanTextMuted)
                }
                Switch(
                    checked = autoRestoreVolume,
                    onCheckedChange = onToggleRestoreVolume,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SultanRed,
                        uncheckedThumbColor = SultanTextMuted,
                        uncheckedTrackColor = SultanSurfaceElevated
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onOpenCapabilityReport,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_capability_report_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VIEW TECHNICAL CAPABILITY REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanCyan)
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SultanDarkBg)
            .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = SultanTextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SultanTextPrimary)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
