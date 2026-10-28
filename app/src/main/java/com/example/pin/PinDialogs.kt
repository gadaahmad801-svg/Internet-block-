package com.example.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.ui.theme.SultanTextMuted
import com.example.ui.theme.SultanTextPrimary
import com.example.ui.theme.SultanTextSecondary
import kotlinx.coroutines.launch

@Composable
fun PinAuthDialog(
    title: String = "PIN REQUIRED",
    subtitle: String = "Enter your PIN to modify protected settings",
    pinManager: PinManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var enteredRecovery by remember { mutableStateOf("") }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SultanAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecoveryMode) Icons.Default.Key else Icons.Default.Lock,
                            contentDescription = null,
                            tint = SultanAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRecoveryMode) "RECOVERY MODE" else title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SultanTextPrimary
                        )
                        Text(
                            text = if (isRecoveryMode) "Enter your secret recovery answer" else subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SultanTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SultanTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (!isRecoveryMode) {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 8) {
                                enteredPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Enter 4-8 Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SultanTextPrimary,
                            unfocusedTextColor = SultanTextPrimary,
                            focusedBorderColor = SultanCyan,
                            unfocusedBorderColor = SultanCardBorder
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = enteredRecovery,
                        onValueChange = {
                            enteredRecovery = it
                            errorMessage = null
                        },
                        label = { Text("Recovery Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SultanTextPrimary,
                            unfocusedTextColor = SultanTextPrimary,
                            focusedBorderColor = SultanCyan,
                            unfocusedBorderColor = SultanCardBorder
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = SultanRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            isRecoveryMode = !isRecoveryMode
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = if (isRecoveryMode) "Back to PIN" else "Forgot PIN?",
                            color = SultanCyan,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (!isRecoveryMode) {
                                    if (pinManager.verifyPin(enteredPin)) {
                                        onSuccess()
                                    } else {
                                        errorMessage = "Incorrect PIN. Try again."
                                    }
                                } else {
                                    if (pinManager.verifyRecoveryAnswer(enteredRecovery)) {
                                        pinManager.resetPin()
                                        onSuccess()
                                    } else {
                                        errorMessage = "Incorrect recovery answer."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SultanCyan.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SultanCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isRecoveryMode) "RESET & UNLOCK" else "VERIFY",
                            color = SultanCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    pinManager: PinManager,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SultanEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SultanEmerald, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONFIGURE PIN LOCK",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SultanTextPrimary
                        )
                        Text(
                            text = "Set a secure PIN and recovery phrase",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SultanTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SultanTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    label = { Text("New PIN (4-8 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SultanTextPrimary,
                        unfocusedTextColor = SultanTextPrimary,
                        focusedBorderColor = SultanCyan,
                        unfocusedBorderColor = SultanCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 8) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SultanTextPrimary,
                        unfocusedTextColor = SultanTextPrimary,
                        focusedBorderColor = SultanCyan,
                        unfocusedBorderColor = SultanCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = { recoveryAnswer = it },
                    label = { Text("Secret Recovery Phrase (for recovery)") },
                    placeholder = { Text("e.g. My childhood pet name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SultanTextPrimary,
                        unfocusedTextColor = SultanTextPrimary,
                        focusedBorderColor = SultanCyan,
                        unfocusedBorderColor = SultanCardBorder
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = SultanRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (pin.length < 4) {
                            errorMessage = "PIN must be at least 4 digits."
                            return@Button
                        }
                        if (pin != confirmPin) {
                            errorMessage = "PINs do not match."
                            return@Button
                        }
                        if (recoveryAnswer.trim().isEmpty()) {
                            errorMessage = "Please enter a recovery phrase."
                            return@Button
                        }

                        coroutineScope.launch {
                            pinManager.setPin(pin, recoveryAnswer)
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SultanEmerald.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SultanEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE PIN & ACTIVATE", color = SultanEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
