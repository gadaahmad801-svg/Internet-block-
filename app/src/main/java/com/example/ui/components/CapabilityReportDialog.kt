package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.SultanAmber
import com.example.ui.theme.SultanCardBorder
import com.example.ui.theme.SultanCyan
import com.example.ui.theme.SultanDarkBg
import com.example.ui.theme.SultanEmerald
import com.example.ui.theme.SultanRed
import com.example.ui.theme.SultanSurface
import com.example.ui.theme.SultanSurfaceCard
import com.example.ui.theme.SultanTextPrimary
import com.example.ui.theme.SultanTextSecondary

@Composable
fun CapabilityReportDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SultanCardBorder, RoundedCornerShape(20.dp)),
            color = SultanSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SultanCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SultanCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYSTEM CAPABILITY MATRIX",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = SultanTextPrimary
                        )
                        Text(
                            text = "Technical Transparency Report",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SultanTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SultanTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TierSection(
                        tierTitle = "1. Standard Unrooted Android (Fully Implemented)",
                        badgeText = "NATIVE & LEGITIMATE",
                        badgeColor = SultanEmerald,
                        items = listOf(
                            CapabilityItem(
                                title = "Device-Wide Internet Blackhole (VPNService)",
                                description = "Routes 0.0.0.0/0 (IPv4), ::/0 (IPv6), and DNS queries to an internal sinkhole socket. Effectively stops YouTube, WhatsApp, Instagram, browsers, and background telemetry without root.",
                                isSupported = true
                            ),
                            CapabilityItem(
                                title = "Continuous Audio Clamp (AudioManager & Observer)",
                                description = "Programmatically mutes all audio streams (Music, Ring, Notification) and immediately forces volume back to 0 whenever a volume change is detected via ContentObserver.",
                                isSupported = true
                            ),
                            CapabilityItem(
                                title = "Active Network Telemetry & Verification",
                                description = "Monitors real-time transport interfaces (Wi-Fi, Cellular, VPN) and executes genuine socket/HTTP probes to verify zero packet leakage.",
                                isSupported = true
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TierSection(
                        tierTitle = "2. Android OS Restrictions (Honest Truth)",
                        badgeText = "OS POLICY",
                        badgeColor = SultanAmber,
                        items = listOf(
                            CapabilityItem(
                                title = "Single Active VPN Rule",
                                description = "Android architecture strictly permits only one active VPN at a time. Activating another VPN will automatically revoke SULTAN BLOCK.",
                                isSupported = true
                            ),
                            CapabilityItem(
                                title = "Hardware Volume Key Interception",
                                description = "An ordinary app cannot physically unbind volume buttons globally. While SULTAN BLOCK clamps volume back to 0 within milliseconds, momentary volume HUD appearance is an OS limitation without Device Owner.",
                                isSupported = true
                            ),
                            CapabilityItem(
                                title = "Direct Wi-Fi Radio Toggle (Android 10+)",
                                description = "Starting with Android 10 (API 29), WifiManager.setWifiEnabled() is blocked for 3rd-party apps to prevent background toggles. SULTAN BLOCK launches the official System Internet Panel.",
                                isSupported = true
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TierSection(
                        tierTitle = "3. Enterprise & Privileged Alternatives",
                        badgeText = "DEVICE OWNER / ROOT",
                        badgeColor = SultanRed,
                        items = listOf(
                            CapabilityItem(
                                title = "Device Owner (MDM / Kiosk Mode)",
                                description = "Required for: DISALLOW_ADJUST_VOLUME (locking volume keys at hardware level) and silent programmatic Wi-Fi power toggling.",
                                isSupported = false
                            ),
                            CapabilityItem(
                                title = "Root Access / Custom Kernel",
                                description = "Required for: iptables/nftables packet dropping without the Android VPN status bar icon.",
                                isSupported = false
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SultanSurfaceCard)
                ) {
                    Text("UNDERSTOOD & ACKNOWLEDGED", color = SultanCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TierSection(
    tierTitle: String,
    badgeText: String,
    badgeColor: Color,
    items: List<CapabilityItem>
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tierTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SultanTextPrimary,
                modifier = Modifier.weight(1f)
            )
            StatusPill(text = badgeText, color = badgeColor, hasDot = false)
        }

        Spacer(modifier = Modifier.height(10.dp))

        items.forEachIndexed { index, item ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (item.isSupported) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (item.isSupported) SultanEmerald else SultanAmber,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SultanTextPrimary
                    )
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = SultanTextSecondary
                    )
                }
            }
        }
    }
}

private data class CapabilityItem(
    val title: String,
    val description: String,
    val isSupported: Boolean
)
