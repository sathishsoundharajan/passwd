package com.sathish.soundharajan.passwd.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// GLASSMORPHISM PREMIUM COLOR SYSTEM
// Vibrant, Translucent, and High-End
// ============================================================================

// Primary Brand Colors (Electric Blue & Purple)
val Primary500 = Color(0xFF2D68FF)
val Primary700 = Color(0xFF1A45B8)

// Accent Colors (Neon)
val AccentPurple = Color(0xFFB026FF)
val AccentCyan = Color(0xFF00E5FF)
val AccentPink = Color(0xFFFF2E93)

// Background Gradients
val BackgroundDarkStart = Color(0xFF0F172A)
val BackgroundDarkEnd = Color(0xFF1E293B)

val BackgroundLightStart = Color(0xFFF1F5F9)
val BackgroundLightEnd = Color(0xFFE2E8F0)

// Glass Surface Colors (Alpha for Blur effect)
val GlassLight = Color(0x99FFFFFF)
val GlassDark = Color(0x20FFFFFF) // Very subtle white on dark

val GlassBorderLight = Color(0x40FFFFFF)
val GlassBorderDark = Color(0x1FFFFFFF)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextWhiteSecondary = Color(0xB3FFFFFF) // 70% opacity
val TextBlack = Color(0xFF111827)
val TextBlackSecondary = Color(0xFF4B5563)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Primary500, AccentPurple)
)

val SecondaryGradient = Brush.horizontalGradient(
    colors = listOf(AccentCyan, Primary500)
)

// Semantic
val ErrorRed = Color(0xFFFF453A)
val SuccessGreen = Color(0xFF32D74B)
val WarningAmber = Color(0xFFFF9F0A)
