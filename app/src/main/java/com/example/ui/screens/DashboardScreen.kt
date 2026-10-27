package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.network.NetworkStatus
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.remember
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
import com.example.ui.MainViewModel
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

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val blockedPackets by viewModel.blockedPackets.collectAsState()
    val blockedBytes by viewModel.blockedBytes.collectAsState()
    val vpnError by viewModel.vpnError.collectAsState()

    val isVolumeLockActive by viewModel.isVolumeLockActive.collectAsState()
    val volumeSuppressionCount by viewModel.volumeSuppressionCount.collectAsState()
    val audioStreams by viewModel.audioStreams.collectAsState()

    val networkStatus by viewModel.networkStatus.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    val autoRestoreInternet by viewModel.autoRestoreInternetOnBoot.collectAsState()
    val autoRestoreVolume by viewModel.autoRestoreVolumeOnBoot.collectAsState()
    val lockMediaOnly by viewModel.lockMediaOnly.collectAsState()

    val showCapabilityDialog by viewModel.showCapabilityDialog.collectAsState()
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

    // Permission launcher for POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkAllPermissions(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkAllPermissions(context)
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
            // App Title Header & Emergency Disarm
            item {
                HeaderSection(
                    isVpnActive = isVpnActive,
                    isVolumeLockActive = isVolumeLockActive,
                    onEmergencyRestore = { viewModel.emergencyRestoreAll(context) }
                )
            }

            // Error Banner if present
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

            // CARD 2: WI-FI CONTROL
            item {
                WifiControlCard(
                    networkStatus = networkStatus,
                    isVpnActive = isVpnActive,
                    onLaunchWifiPanel = { viewModel.openSystemWifiSettings(context) }
                )
            }

            // CARD 3: VOLUME LOCK
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

            // CARD 4: SYSTEM STATUS & REAL PROBE
            item {
                SystemStatusCard(
                    networkStatus = networkStatus,
                    isVpnActive = isVpnActive,
                    onProbe = { viewModel.runConnectivityProbe() }
                )
            }

            // CARD 5: PERMISSIONS & AUTHORIZATION
            item {
                PermissionsCard(
                    permissions = permissions,
                    onRequestPermission = { item ->
                        if (item.permission == "android.net.VpnService") {
                            val prep = VpnService.prepare(context)
                            if (prep != null) {
                                vpnLauncher.launch(prep)
                            }
                        } else {
                            permissionLauncher.launch(item.permission)
                        }
                    }
                )
            }

            // CARD 6: SETTINGS & CAPABILITY REPORT
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

    if (showCapabilityDialog) {
        CapabilityReportDialog(onDismiss = { viewModel.setShowCapabilityDialog(false) })
    }
}

@Composable
private fun HeaderSection(
    isVpnActive: Boolean,
    isVolumeLockActive: Boolean,
    onEmergencyRestore: () -> Unit
) {
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
                        .background(if (isVpnActive || isVolumeLockActive) SultanRed else SultanEmerald)
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

        if (isVpnActive || isVolumeLockActive) {
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
        // Master Toggle Row
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
                Text(
                    text = "MASTER INTERNET BLOCK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SultanTextPrimary
                )
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

        Spacer(modifier = Modifier.height(14.dp))

        // Metrics Grid
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

        Spacer(modifier = Modifier.height(12.dp))

        // Scope description
        Text(
            text = "Enforces zero-data leakage on YouTube, Instagram, WhatsApp, browsers, games, VoIP calls, and background OS tasks using legitimate Android VpnService.",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = SultanTextMuted
        )
    }
}

@Composable
private fun WifiControlCard(
    networkStatus: NetworkStatus,
    isVpnActive: Boolean,
    onLaunchWifiPanel: () -> Unit
) {
    CyberCard(
        title = "WI-FI CONTROL",
        subtitle = "Network interface monitoring & system controls",
        icon = if (networkStatus.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
        iconTint = if (networkStatus.isWifiConnected) SultanCyan else SultanTextMuted,
        trailingBadge = {
            StatusPill(
                text = if (networkStatus.isWifiConnected) "CONNECTED" else if (networkStatus.isWifiEnabled) "RADIO ON" else "RADIO OFF",
                color = if (networkStatus.isWifiConnected) SultanCyan else SultanTextMuted
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SultanDarkBg)
                .border(1.dp, SultanCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = networkStatus.wifiSsid ?: "No Wi-Fi Network",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SultanTextPrimary
                    )
                    Text(
                        text = if (networkStatus.isWifiConnected) {
                            "Signal: ${networkStatus.wifiRssi} dBm | Speed: ${networkStatus.wifiLinkSpeed} Mbps"
                        } else {
                            "Wi-Fi interface is currently inactive"
                        },
                        fontSize = 11.sp,
                        color = SultanTextSecondary
                    )
                }

                StatusPill(
                    text = if (isVpnActive) "TRAFFIC SINKED" else "PASSING",
                    color = if (isVpnActive) SultanRed else SultanEmerald,
                    hasDot = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunchWifiPanel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_wifi_panel_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SultanSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, SultanCardBorder)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN SYSTEM WI-FI / INTERNET PANEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SultanTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Android 10+ OS Policy: Apps cannot silently disable the physical Wi-Fi radio without enterprise Device Owner privileges. Use the panel button above or master Internet Block.",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = SultanTextMuted
        )
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
        // Master Toggle Row
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
                Text(
                    text = "MASTER VOLUME LOCK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SultanTextPrimary
                )
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

        Spacer(modifier = Modifier.height(14.dp))

        // Suppression Stats & Stream snapshot
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

        Spacer(modifier = Modifier.height(12.dp))

        // Streams Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SultanDarkBg)
                .padding(10.dp)
        ) {
            Text("TARGET AUDIO STREAMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
            Spacer(modifier = Modifier.height(6.dp))

            if (audioStreams.isEmpty()) {
                Text("Engage Volume Lock to inspect live streams", fontSize = 11.sp, color = SultanTextSecondary)
            } else {
                audioStreams.take(4).forEach { stream ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stream.name, fontSize = 12.sp, color = SultanTextPrimary)
                        Text(
                            text = if (isVolumeLockActive) "0 / ${stream.maxVolume} (MUTED)" else "${stream.currentVolume} / ${stream.maxVolume}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVolumeLockActive) SultanRed else SultanEmerald
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle media only vs all
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Lock Media Only (Keep Ringtone)",
                fontSize = 12.sp,
                color = SultanTextSecondary
            )
            Switch(
                checked = lockMediaOnly,
                onCheckedChange = onToggleMediaOnly,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SultanCyan,
                    uncheckedThumbColor = SultanTextMuted,
                    uncheckedTrackColor = SultanSurfaceElevated
                )
            )
        }
    }
}

@Composable
private fun SystemStatusCard(
    networkStatus: NetworkStatus,
    isVpnActive: Boolean,
    onProbe: () -> Unit
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

            // Probe verification section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "REAL-TIME LEAKAGE PROBE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SultanTextPrimary
                    )
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
        }
    }
}

@Composable
private fun PermissionsCard(
    permissions: List<com.example.ui.PermissionItem>,
    onRequestPermission: (com.example.ui.PermissionItem) -> Unit
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
