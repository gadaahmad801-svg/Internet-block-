package com.example.certainlock

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.SultanCardBorder
import com.example.ui.theme.SultanCyan
import com.example.ui.theme.SultanDarkBg
import com.example.ui.theme.SultanEmerald
import com.example.ui.theme.SultanRed
import com.example.ui.theme.SultanSurface
import com.example.ui.theme.SultanSurfaceCard
import com.example.ui.theme.SultanTextMuted
import com.example.ui.theme.SultanTextPrimary
import com.example.ui.theme.SultanTextSecondary

@Composable
fun AddAppsDialog(
    installedApps: List<InstalledAppItem>,
    onToggleApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val query = searchQuery.trim().lowercase()
            installedApps.filter {
                it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
    }

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
                            .background(SultanRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = SultanRed, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SELECT RESTRICTED APPS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SultanTextPrimary
                        )
                        Text(
                            text = "${installedApps.count { it.isSelected }} applications protected",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SultanTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SultanTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Instagram, ChatGPT, BGMI, browsers...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SultanTextMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SultanTextPrimary,
                        unfocusedTextColor = SultanTextPrimary,
                        focusedBorderColor = SultanCyan,
                        unfocusedBorderColor = SultanCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // App list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppSelectionRow(
                            app = app,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SultanRed.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SultanRed)
                ) {
                    Text("DONE", color = SultanRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AppSelectionRow(
    app: InstalledAppItem,
    onToggle: () -> Unit
) {
    val bitmap = remember(app.icon) {
        try {
            app.icon?.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (app.isSelected) SultanRed.copy(alpha = 0.12f) else SultanDarkBg)
            .border(
                1.dp,
                if (app.isSelected) SultanRed.copy(alpha = 0.5f) else SultanCardBorder.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SultanSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = SultanTextMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SultanTextPrimary
            )
            Text(
                text = app.packageName,
                fontSize = 10.sp,
                color = SultanTextMuted,
                maxLines = 1
            )
        }

        Checkbox(
            checked = app.isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = SultanRed,
                uncheckedColor = SultanCardBorder,
                checkmarkColor = Color.White
            )
        )
    }
}
