package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SultanColorScheme = darkColorScheme(
    primary = SultanRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0B14),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = SultanEmerald,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00391A),
    onSecondaryContainer = Color(0xFF75FFB1),
    tertiary = SultanCyan,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF00373D),
    onTertiaryContainer = Color(0xFF9CF4FF),
    background = SultanDarkBg,
    onBackground = SultanTextPrimary,
    surface = SultanSurface,
    onSurface = SultanTextPrimary,
    surfaceVariant = SultanSurfaceCard,
    onSurfaceVariant = SultanTextSecondary,
    outline = SultanCardBorder
)

@Composable
fun SultanBlockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SultanColorScheme,
        typography = Typography,
        content = content
    )
}
