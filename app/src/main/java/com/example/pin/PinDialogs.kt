package com.example.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.SultanAmber
import com.example.ui.theme.SultanEmerald
import com.example.ui.theme.SultanRed
import kotlinx.coroutines.launch

/**
 * 6-Dot visual indicator for 6-Digit PIN
 */
@Composable
fun PinDotsIndicator(
    pinLength: Int,
    maxDigits: Int = 6,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pinLength
            val dotSize by animateDpAsState(
                targetValue = if (isFilled) 16.dp else 14.dp,
                animationSpec = tween(durationMillis = 150),
                label = "dot_size"
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> SultanRed
                    isFilled -> extendedColors.accentBlue
                    else -> extendedColors.cardElevated
                },
                animationSpec = tween(durationMillis = 150),
                label = "dot_color"
            )
            val borderColor = when {
                isError -> SultanRed
                isFilled -> extendedColors.accentBlue
                else -> extendedColors.border
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.5.dp, borderColor, CircleShape)
            )
        }
    }
}

/**
 * Clean 3x4 numeric keypad specifically designed for 6-Digit PIN entry
 */
@Composable
fun NumericPinKeypad(
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', '<')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { key ->
                    when (key) {
                        null -> {
                            Spacer(modifier = Modifier.size(64.dp))
                        }
                        '<' -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(extendedColors.cardElevated)
                                    .clickable(onClick = onDeleteClick)
                                    .testTag("pin_delete_key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete Digit",
                                    tint = extendedColors.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(extendedColors.cardElevated)
                                    .border(1.dp, extendedColors.border, CircleShape)
                                    .clickable(onClick = { onDigitClick(key) })
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = extendedColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6-Digit PIN Authentication Dialog for protected features
 */
@Composable
fun PinAuthDialog(
    title: String = "ENTER 6-DIGIT PIN",
    subtitle: String = "Authenticate to access protected security controls",
    pinManager: PinManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val extendedColors = LocalExtendedColors.current

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, extendedColors.border, RoundedCornerShape(24.dp)),
            color = extendedColors.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(extendedColors.accentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecoveryMode) Icons.Default.Key else Icons.Default.Lock,
                            contentDescription = null,
                            tint = extendedColors.accentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRecoveryMode) "EMERGENCY RECOVERY" else title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = extendedColors.textPrimary
                        )
                        Text(
                            text = if (isRecoveryMode) "Provide your secret recovery answer to reset PIN" else subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = extendedColors.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = extendedColors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isRecoveryMode) {
                    // 6 Dots Indicator
                    PinDotsIndicator(
                        pinLength = enteredPin.length,
                        maxDigits = 6,
                        isError = errorMessage != null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = SultanRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "6 Digits Required",
                            color = extendedColors.textMuted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Keypad
                    NumericPinKeypad(
                        onDigitClick = { digit ->
                            if (enteredPin.length < 6) {
                                val next = enteredPin + digit
                                enteredPin = next
                                errorMessage = null

                                if (next.length == 6) {
                                    coroutineScope.launch {
                                        if (pinManager.verifyPin(next)) {
                                            onSuccess()
                                        } else {
                                            errorMessage = "Incorrect PIN. Try again."
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                errorMessage = null
                            }
                        }
                    )
                } else {
                    // Recovery Answer Mode
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = {
                            recoveryAnswer = it
                            errorMessage = null
                        },
                        label = { Text("Secret Recovery Phrase") },
                        placeholder = { Text("Enter phrase saved during PIN setup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary,
                            focusedBorderColor = extendedColors.accentBlue,
                            unfocusedBorderColor = extendedColors.border
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = SultanRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (pinManager.verifyRecoveryAnswer(recoveryAnswer)) {
                                    pinManager.resetPin()
                                    onSuccess()
                                } else {
                                    errorMessage = "Recovery phrase incorrect."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VERIFY & RESET PIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        isRecoveryMode = !isRecoveryMode
                        errorMessage = null
                        enteredPin = ""
                    }
                ) {
                    Text(
                        text = if (isRecoveryMode) "Back to Numeric PIN" else "Forgot 6-Digit PIN?",
                        color = extendedColors.accentBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 6-Digit PIN Setup Flow:
 * Step 1: "CREATE YOUR 6-DIGIT PIN"
 * Step 2: "CONFIRM YOUR 6-DIGIT PIN"
 * Step 3: Secret Recovery Phrase
 */
@Composable
fun PinSetupDialog(
    pinManager: PinManager,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val extendedColors = LocalExtendedColors.current

    var step by remember { mutableIntStateOf(1) } // 1: Create, 2: Confirm, 3: Recovery
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, extendedColors.border, RoundedCornerShape(24.dp)),
            color = extendedColors.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(extendedColors.accentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = extendedColors.accentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (step) {
                                1 -> "CREATE YOUR 6-DIGIT PIN"
                                2 -> "CONFIRM YOUR 6-DIGIT PIN"
                                else -> "RECOVERY PHRASE"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = extendedColors.textPrimary
                        )
                        Text(
                            text = when (step) {
                                1 -> "Enter a new 6-digit PIN"
                                2 -> "Re-enter the exact same 6-digit PIN"
                                else -> "Enter a recovery answer for safe unlock"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = extendedColors.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = extendedColors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (step) {
                    1 -> {
                        // Step 1: CREATE
                        PinDotsIndicator(
                            pinLength = firstPin.length,
                            maxDigits = 6,
                            isError = errorMessage != null
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (errorMessage != null) {
                            Text(errorMessage!!, color = SultanRed, fontSize = 12.sp)
                        } else {
                            Text("Step 1 of 3", color = extendedColors.textMuted, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        NumericPinKeypad(
                            onDigitClick = { digit ->
                                if (firstPin.length < 6) {
                                    val next = firstPin + digit
                                    firstPin = next
                                    errorMessage = null
                                    if (next.length == 6) {
                                        step = 2
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (firstPin.isNotEmpty()) {
                                    firstPin = firstPin.dropLast(1)
                                    errorMessage = null
                                }
                            }
                        )
                    }
                    2 -> {
                        // Step 2: CONFIRM
                        PinDotsIndicator(
                            pinLength = confirmPin.length,
                            maxDigits = 6,
                            isError = errorMessage != null
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (errorMessage != null) {
                            Text(errorMessage!!, color = SultanRed, fontSize = 12.sp)
                        } else {
                            Text("Step 2 of 3", color = extendedColors.textMuted, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        NumericPinKeypad(
                            onDigitClick = { digit ->
                                if (confirmPin.length < 6) {
                                    val next = confirmPin + digit
                                    confirmPin = next
                                    errorMessage = null
                                    if (next.length == 6) {
                                        if (next == firstPin) {
                                            step = 3
                                        } else {
                                            errorMessage = "PINs do not match. Please try again."
                                            confirmPin = ""
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                    errorMessage = null
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                step = 1
                                firstPin = ""
                                confirmPin = ""
                                errorMessage = null
                            }
                        ) {
                            Text("Start Over", color = extendedColors.accentBlue, fontSize = 12.sp)
                        }
                    }
                    3 -> {
                        // Step 3: RECOVERY PHRASE
                        Text(
                            text = "Set a secret phrase used to reset your 6-digit PIN in case you forget it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = {
                                recoveryAnswer = it
                                errorMessage = null
                            },
                            label = { Text("Secret Recovery Phrase") },
                            placeholder = { Text("e.g. Favorite book, birthplace, etc.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = extendedColors.textPrimary,
                                unfocusedTextColor = extendedColors.textPrimary,
                                focusedBorderColor = extendedColors.accentBlue,
                                unfocusedBorderColor = extendedColors.border
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = SultanRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (recoveryAnswer.trim().isEmpty()) {
                                    errorMessage = "Please enter a secret recovery phrase."
                                    return@Button
                                }
                                coroutineScope.launch {
                                    pinManager.setPin(firstPin, recoveryAnswer)
                                    onComplete()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE 6-DIGIT PIN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
