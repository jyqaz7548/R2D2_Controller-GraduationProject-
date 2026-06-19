package com.r2d2.controller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 네온 사이버 팔레트 (웹 앱과 동일 컨셉)
val NeonCyan    = Color(0xFF00FFEE)
val NeonGreen   = Color(0xFF39FF14)
val DarkBg      = Color(0xFF0A0E1A)
val CardBg      = Color(0xFF111827)
val BorderColor = Color(0xFF1F2937)
val MutedText   = Color(0xFF6B7280)
val DangerRed   = Color(0xFFEF4444)

private val DarkColors = darkColorScheme(
    primary          = NeonCyan,
    secondary        = NeonGreen,
    background       = DarkBg,
    surface          = CardBg,
    onBackground     = Color.White,
    onSurface        = Color.White,
    outline          = BorderColor,
    error            = DangerRed,
)

@Composable
fun R2D2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content,
    )
}
