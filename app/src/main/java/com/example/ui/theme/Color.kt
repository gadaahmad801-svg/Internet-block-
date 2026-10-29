package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Shared Blue Accent (Consistent across Dark and Light modes)
val SultanBlue = Color(0xFF2563EB)
val SultanBlueAccent = Color(0xFF3B82F6)
val SultanBlueGlow = Color(0x332563EB)

// Alert / Protection Accent Colors
val SultanRed = Color(0xFFEF4444)
val SultanRedGlow = Color(0x33EF4444)
val SultanEmerald = Color(0xFF10B981)
val SultanEmeraldGlow = Color(0x3310B981)
val SultanAmber = Color(0xFFF59E0B)

// Dark Mode Palette (Dark Charcoal background, slightly lighter cards, crisp white text)
val DarkCharcoalBg = Color(0xFF121214)
val DarkCharcoalSurface = Color(0xFF1A1A1E)
val DarkCharcoalCard = Color(0xFF232328)
val DarkCharcoalCardElevated = Color(0xFF2D2D33)
val DarkCardBorder = Color(0xFF383842)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFA1A1AA)
val DarkTextMuted = Color(0xFF71717A)

// Light Mode Palette (Clean light-gray background, clean cards, dark text)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFF1F5F9)
val LightCardElevated = Color(0xFFE2E8F0)
val LightCardBorder = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

// Compatibility aliases mapped to consistent Dark Charcoal & Blue accent
val SultanDarkBg = DarkCharcoalBg
val SultanSurface = DarkCharcoalSurface
val SultanSurfaceCard = DarkCharcoalCard
val SultanSurfaceElevated = DarkCharcoalCardElevated
val SultanCardBorder = DarkCardBorder
val SultanTextPrimary = DarkTextPrimary
val SultanTextSecondary = DarkTextSecondary
val SultanTextMuted = DarkTextMuted
val SultanCyan = SultanBlueAccent
