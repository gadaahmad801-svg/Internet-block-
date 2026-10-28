package com.example.schedule

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import java.util.Calendar

@Composable
fun ScheduleDialog(
    initialItem: ScheduleItem? = null,
    onSave: (ScheduleItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "Focus Schedule") }
    var startHour by remember { mutableStateOf(initialItem?.startHour ?: 22) }
    var startMinute by remember { mutableStateOf(initialItem?.startMinute ?: 0) }
    var endHour by remember { mutableStateOf(initialItem?.endHour ?: 7) }
    var endMinute by remember { mutableStateOf(initialItem?.endMinute ?: 0) }

    var selectedDays by remember {
        mutableStateOf(
            initialItem?.daysOfWeek ?: setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        )
    }

    var controlInternet by remember { mutableStateOf(initialItem?.controlInternet ?: true) }
    var controlVolume by remember { mutableStateOf(initialItem?.controlVolume ?: true) }
    var controlAppLock by remember { mutableStateOf(initialItem?.controlAppLock ?: true) }

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
                    .verticalScroll(rememberScrollState())
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
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = SultanCyan, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialItem != null) "EDIT SCHEDULE" else "NEW SCHEDULE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SultanTextPrimary
                        )
                        Text(
                            text = "Automate protection during set hours",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SultanTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SultanTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Schedule Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SultanTextPrimary,
                        unfocusedTextColor = SultanTextPrimary,
                        focusedBorderColor = SultanCyan,
                        unfocusedBorderColor = SultanCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Time Pickers (Start & End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeSelectorCard(
                        label = "START TIME",
                        hour = startHour,
                        minute = startMinute,
                        onHourChange = { startHour = (it + 24) % 24 },
                        onMinuteChange = { startMinute = (it + 60) % 60 },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectorCard(
                        label = "END TIME",
                        hour = endHour,
                        minute = endMinute,
                        onHourChange = { endHour = (it + 24) % 24 },
                        onMinuteChange = { endMinute = (it + 60) % 60 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("REPEAT DAYS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                val daysMap = listOf(
                    Calendar.MONDAY to "M",
                    Calendar.TUESDAY to "T",
                    Calendar.WEDNESDAY to "W",
                    Calendar.THURSDAY to "T",
                    Calendar.FRIDAY to "F",
                    Calendar.SATURDAY to "S",
                    Calendar.SUNDAY to "S"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysMap.forEach { (calDay, label) ->
                        val isSelected = selectedDays.contains(calDay)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SultanCyan else SultanDarkBg)
                                .border(1.dp, if (isSelected) SultanCyan else SultanCardBorder, CircleShape)
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                                    } else {
                                        selectedDays + calDay
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else SultanTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("PROTECTION FEATURES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                FeatureToggleRow("Block Internet Traffic", controlInternet) { controlInternet = it }
                FeatureToggleRow("Lock System Volume (Mute)", controlVolume) { controlVolume = it }
                FeatureToggleRow("Certain Lock (Restricted Apps)", controlAppLock) { controlAppLock = it }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val schedule = ScheduleItem(
                            id = initialItem?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.ifBlank { "Scheduled Protection" },
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            daysOfWeek = selectedDays,
                            isEnabled = initialItem?.isEnabled ?: true,
                            controlInternet = controlInternet,
                            controlVolume = controlVolume,
                            controlAppLock = controlAppLock
                        )
                        onSave(schedule)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan)
                ) {
                    Text("SAVE SCHEDULE", color = SultanCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimeSelectorCard(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SultanDarkBg)
            .border(1.dp, SultanCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▲", fontSize = 10.sp, color = SultanCyan, modifier = Modifier.clickable { onHourChange(hour + 1) })
                Text(String.format("%02d", hour), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                Text("▼", fontSize = 10.sp, color = SultanCyan, modifier = Modifier.clickable { onHourChange(hour - 1) })
            }
            Text(" : ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SultanTextMuted)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▲", fontSize = 10.sp, color = SultanCyan, modifier = Modifier.clickable { onMinuteChange(minute + 5) })
                Text(String.format("%02d", minute), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SultanTextPrimary)
                Text("▼", fontSize = 10.sp, color = SultanCyan, modifier = Modifier.clickable { onMinuteChange(minute - 5) })
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = SultanTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SultanCyan,
                uncheckedThumbColor = SultanTextMuted,
                uncheckedTrackColor = SultanSurfaceElevated
            )
        )
    }
}
