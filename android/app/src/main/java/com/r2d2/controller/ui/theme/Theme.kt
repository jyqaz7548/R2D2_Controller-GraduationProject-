package com.r2d2.controller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 깡통 로봇 팔레트 ────────────────────────────────────────────────────────
val SteelDark     = Color(0xFF1A1A1C)   // 배경 — 다크 아이언
val SteelMid      = Color(0xFF252527)   // 카드 / 패널
val SteelLight    = Color(0xFF3C3C40)   // 구분선 / 테두리
val RustOrange    = Color(0xFFD4703A)   // 주 강조 — 녹슨 오렌지
val AmberYellow   = Color(0xFFD4A843)   // 보조 강조 — 호박색
val CreamText     = Color(0xFFCFC5AC)   // 메인 텍스트 — 크림
val MutedBrown    = Color(0xFF7A7060)   // 보조 텍스트
val DangerRed     = Color(0xFFAA3333)   // 위험
val MilitaryGreen = Color(0xFF6B8C5A)   // 연결됨 / 추적 활성

// 하위 호환 alias (ControllerScreen 참조용)
val DarkBg      = SteelDark
val CardBg      = SteelMid
val BorderColor = SteelLight
val MutedText   = MutedBrown
val NeonCyan    = AmberYellow   // 조이스틱 색
val NeonGreen   = MilitaryGreen
val NeonOrange  = RustOrange

private val TinRobotColors = darkColorScheme(
    primary      = RustOrange,
    secondary    = AmberYellow,
    background   = SteelDark,
    surface      = SteelMid,
    onBackground = CreamText,
    onSurface    = CreamText,
    outline      = SteelLight,
    error        = DangerRed,
)

@Composable
fun R2D2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TinRobotColors,
        content     = content,
    )
}
