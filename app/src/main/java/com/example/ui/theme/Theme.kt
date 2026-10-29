package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

data class ExtendedThemeColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val isDark: Boolean
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedThemeColors(
        bg = DarkCharcoalBg,
        surface = DarkCharcoalSurface,
        card = DarkCharcoalCard,
        cardElevated = DarkCharcoalCardElevated,
        border = DarkCardBorder,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textMuted = DarkTextMuted,
        accentBlue = SultanBlueAccent,
        isDark = true
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = SultanBlueAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SultanEmerald,
    onSecondary = Color.Black,
    tertiary = SultanAmber,
    background = DarkCharcoalBg,
    onBackground = DarkTextPrimary,
    surface = DarkCharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCharcoalCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SultanBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SultanEmerald,
    onSecondary = Color.White,
    tertiary = SultanAmber,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder
)

@Composable
fun SultanBlockTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val extendedColors = if (isDark) {
        ExtendedThemeColors(
            bg = DarkCharcoalBg,
            surface = DarkCharcoalSurface,
            card = DarkCharcoalCard,
            cardElevated = DarkCharcoalCardElevated,
            border = DarkCardBorder,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textMuted = DarkTextMuted,
            accentBlue = SultanBlueAccent,
            isDark = true
        )
    } else {
        ExtendedThemeColors(
            bg = LightBg,
            surface = LightSurface,
            card = LightCard,
            cardElevated = LightCardElevated,
            border = LightCardBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            accentBlue = SultanBlue,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
