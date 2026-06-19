package com.r2d2.controller.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.r2d2.controller.bluetooth.BtState
import com.r2d2.controller.ui.theme.*
import com.r2d2.controller.viewmodel.BODY_MAX_DEG
import com.r2d2.controller.viewmodel.SpeedPreset

// ─── 패널 외곽선 색 ───────────────────────────────────────────────────────────
private val PanelBorder  = Color(0xFF4A4640)
private val PanelInner   = Color(0xFF2E2C2A)
private val BoltColor    = Color(0xFF5A5650)
private val ActiveAmber  = AmberYellow
private val ActiveGreen  = MilitaryGreen
private val ActiveRust   = RustOrange

@Composable
fun ControllerScreen(
    btState: BtState,
    isManualMode: Boolean,
    isTracking: Boolean,
    speed: SpeedPreset,
    pairedDevices: List<BluetoothDevice>,
    bodyAngle: Int,
    targetBodyAngle: Int,
    bodyLimitError: Boolean,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onJoystickRelease: () -> Unit,
    onToggleMode: () -> Unit,
    onSetSpeed: (SpeedPreset) -> Unit,
    onSetTargetBodyAngle: (Int) -> Unit,
    onBodyHome: () -> Unit,
    onSayHello: () -> Unit,
    onPlayMusic: () -> Unit,
    onDance: () -> Unit,
) {
    var showDevicePicker by remember { mutableStateOf(false) }
    val isConnected = btState is BtState.Connected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SteelDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── 상태 바 ──────────────────────────────────────────────────
        TinStatusBar(
            btState        = btState,
            onConnectClick = {
                if (isConnected) onDisconnect()
                else showDevicePicker = true
            },
        )

        // ── 모드 토글 ────────────────────────────────────────────────
        TinModeToggle(isManualMode = isManualMode, onToggle = onToggleMode)

        PanelDivider()

        // ── 속도 프리셋 ──────────────────────────────────────────────
        AnimatedVisibility(visible = isManualMode) {
            SpeedPanel(speed = speed, onSetSpeed = onSetSpeed)
        }

        PanelDivider()

        // ── 조이스틱 / 트래킹 ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 300.dp)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isManualMode) {
                VirtualJoystick(
                    onMove    = onJoystickMove,
                    onRelease = onJoystickRelease,
                )
            } else {
                TinTrackingView(isTracking = isTracking)
            }
        }

        PanelDivider()

        // ── 상체 슬라이더 ────────────────────────────────────────────
        if (isManualMode) {
            TinBodyControl(
                currentAngle = bodyAngle,
                targetAngle  = targetBodyAngle,
                limitError   = bodyLimitError,
                onSetTarget  = onSetTargetBodyAngle,
                onHome       = onBodyHome,
            )
            PanelDivider()
        }

        // ── 사운드 패널 ──────────────────────────────────────────────
        TinSoundPanel(
            onSayHello  = onSayHello,
            onPlayMusic = onPlayMusic,
            onDance     = onDance,
        )

        Spacer(Modifier.height(16.dp))
    }

    if (showDevicePicker) {
        TinDevicePicker(
            devices   = pairedDevices,
            onSelect  = { device -> showDevicePicker = false; onConnect(device) },
            onDismiss = { showDevicePicker = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공통 유틸
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PanelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(PanelBorder),
    )
}

@Composable
private fun RivetRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(BoltColor),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 상태 바
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TinStatusBar(btState: BtState, onConnectClick: () -> Unit) {
    val isConnected  = btState is BtState.Connected
    val isConnecting = btState is BtState.Connecting

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "blink",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelInner),
    ) {
        RivetRow()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 상태 표시등
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected  -> ActiveGreen.copy(alpha = blinkAlpha)
                            isConnecting -> ActiveAmber.copy(alpha = blinkAlpha)
                            else         -> Color(0xFF3A3A3A)
                        }
                    )
                    .border(1.5.dp, PanelBorder, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = when {
                    isConnecting -> "CONNECTING..."
                    isConnected  -> "CONNECTED"
                    else         -> "OFFLINE"
                },
                color      = if (isConnected) ActiveGreen else MutedBrown,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.weight(1f))

            // 연결 / 해제 버튼
            val btnColor = if (isConnected) DangerRed else ActiveGreen
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(btnColor.copy(alpha = 0.15f))
                    .border(1.5.dp, btnColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .clickable(enabled = !isConnecting) { onConnectClick() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    when {
                        isConnecting -> {
                            CircularProgressIndicator(Modifier.size(12.dp), color = ActiveAmber, strokeWidth = 2.dp)
                            Text("WAIT", color = ActiveAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        isConnected -> {
                            Icon(Icons.Filled.PowerOff, null, tint = DangerRed, modifier = Modifier.size(13.dp))
                            Text("DISCONNECT", color = DangerRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(Icons.Filled.Power, null, tint = ActiveGreen, modifier = Modifier.size(13.dp))
                            Text("CONNECT", color = ActiveGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        RivetRow()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 모드 토글
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TinModeToggle(isManualMode: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SteelMid)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "[ CONTROL MODE ]",
            color      = MutedBrown,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier   = Modifier.align(Alignment.CenterHorizontally),
        )

        // 토글 — 두꺼운 사각 버튼 두 개
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, PanelBorder, RoundedCornerShape(6.dp)),
        ) {
            TinToggleSegment(
                label    = "MANUAL",
                icon     = Icons.Filled.SportsEsports,
                active   = isManualMode,
                color    = ActiveAmber,
                onClick  = { if (!isManualMode) onToggle() },
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.width(2.dp).height(56.dp).background(PanelBorder))
            TinToggleSegment(
                label    = "AUTO",
                icon     = Icons.Filled.Visibility,
                active   = !isManualMode,
                color    = ActiveGreen,
                onClick  = { if (isManualMode) onToggle() },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text    = if (isManualMode) "수동 조종 모드 — 조이스틱으로 직접 제어" else "자율 추적 모드 — HuskyLens AI 카메라",
            color   = MutedBrown,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TinToggleSegment(
    label: String,
    icon: ImageVector,
    active: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(if (active) color.copy(alpha = 0.18f) else SteelMid)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = if (active) color else MutedBrown,
                modifier = Modifier.size(18.dp),
            )
            Text(
                label,
                color      = if (active) color else MutedBrown,
                fontSize   = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 속도 패널
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpeedPanel(speed: SpeedPreset, onSetSpeed: (SpeedPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SteelMid)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "SPD",
            color      = MutedBrown,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        SpeedPreset.entries.forEach { preset ->
            val active = speed == preset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) ActiveRust.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (active) 2.dp else 1.dp,
                        color = if (active) ActiveRust else PanelBorder,
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clickable { onSetSpeed(preset) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    preset.label,
                    color      = if (active) ActiveRust else MutedBrown,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 트래킹 뷰 (자동 모드)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TinTrackingView(isTracking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "blink",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "[ HUSKYLENS TRACKING ]",
            color      = MutedBrown,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )

        // 뷰파인더
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0E0E10))
                .border(
                    3.dp,
                    if (isTracking) ActiveGreen.copy(alpha = 0.8f) else PanelBorder,
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val gridColor = ActiveGreen.copy(alpha = 0.08f)
                for (i in 1..2) {
                    drawLine(gridColor, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), 1f)
                    drawLine(gridColor, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
                }
                // 스캔라인
                val scanY = h * scanProgress
                drawLine(ActiveGreen.copy(alpha = 0.5f), Offset(0f, scanY), Offset(w, scanY), 1.5f)

                // 코너 브래킷
                val m = 12f; val bl = 20f; val bw = 2.5f
                val bc = (if (isTracking) ActiveGreen else MutedBrown).copy(alpha = 0.9f)
                drawLine(bc, Offset(m, m), Offset(m + bl, m), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(m, m), Offset(m, m + bl), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(w - m, m), Offset(w - m - bl, m), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(w - m, m), Offset(w - m, m + bl), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(m, h - m), Offset(m + bl, h - m), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(m, h - m), Offset(m, h - m - bl), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(w - m, h - m), Offset(w - m - bl, h - m), bw, cap = StrokeCap.Square)
                drawLine(bc, Offset(w - m, h - m), Offset(w - m, h - m - bl), bw, cap = StrokeCap.Square)
            }

            Icon(
                imageVector = if (isTracking) Icons.Filled.GpsFixed else Icons.Filled.RemoveRedEye,
                contentDescription = null,
                tint     = if (isTracking) ActiveGreen.copy(alpha = blinkAlpha) else MutedBrown,
                modifier = Modifier.size(52.dp),
            )
        }

        // 상태 텍스트
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTracking) ActiveGreen.copy(alpha = blinkAlpha) else MutedBrown
                    ),
            )
            Text(
                text       = if (isTracking) "TARGET LOCKED" else "SCANNING...",
                color      = if (isTracking) ActiveGreen else MutedBrown,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 상체 제어
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TinBodyControl(
    currentAngle: Int,
    targetAngle: Int,
    limitError: Boolean,
    onSetTarget: (Int) -> Unit,
    onHome: () -> Unit,
) {
    var sliderValue by remember(targetAngle) { mutableFloatStateOf(targetAngle.toFloat()) }
    val lastSendMs = remember { longArrayOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SteelMid)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "[ TORSO ROTATION ]",
                color      = MutedBrown,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SteelLight.copy(alpha = 0.3f))
                    .border(1.5.dp, PanelBorder, RoundedCornerShape(4.dp))
                    .clickable { onHome() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Home, null, tint = CreamText, modifier = Modifier.size(13.dp))
                    Text("HOME", color = CreamText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 에러
        AnimatedVisibility(visible = limitError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DangerRed.copy(alpha = 0.15f))
                    .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, null, tint = DangerRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("LIMIT EXCEEDED", color = DangerRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        // 각도 뱃지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(SteelDark)
                .border(1.dp, PanelBorder, RoundedCornerShape(4.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TinAngleBadge("CURRENT", currentAngle, ActiveGreen)
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(PanelBorder))
            TinAngleBadge("TARGET", sliderValue.toInt(), ActiveAmber)
        }

        // 슬라이더
        Column {
            Slider(
                value         = sliderValue,
                onValueChange = { newVal ->
                    sliderValue = newVal
                    val now = System.currentTimeMillis()
                    if (now - lastSendMs[0] >= 80) {
                        lastSendMs[0] = now
                        onSetTarget(newVal.toInt().coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG))
                    }
                },
                onValueChangeFinished = {
                    onSetTarget(sliderValue.toInt().coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG))
                },
                valueRange = (-BODY_MAX_DEG).toFloat()..BODY_MAX_DEG.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor         = ActiveRust,
                    activeTrackColor   = ActiveRust,
                    inactiveTrackColor = SteelLight,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("-${BODY_MAX_DEG}°", color = MutedBrown, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("0°",               color = MutedBrown, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("+${BODY_MAX_DEG}°", color = MutedBrown, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        val delta = kotlin.math.abs(sliderValue.toInt() - currentAngle)
        if (delta >= 3) {
            val dir = if (sliderValue.toInt() > currentAngle) "CW ▶" else "◀ CCW"
            Text(
                "ROTATING $dir  ${delta}° remaining",
                color      = ActiveAmber,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun TinAngleBadge(label: String, angle: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedBrown, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            "${angle}°",
            color      = color,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 사운드 패널
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TinSoundPanel(
    onSayHello: () -> Unit,
    onPlayMusic: () -> Unit,
    onDance: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelInner),
    ) {
        RivetRow()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "[ SOUND PANEL ]",
                color      = MutedBrown,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier   = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TinSoundButton(
                    icon    = Icons.Filled.PanTool,
                    label   = "HELLO",
                    color   = AmberYellow,
                    onClick = onSayHello,
                    modifier = Modifier.weight(1f),
                )
                TinSoundButton(
                    icon    = Icons.Filled.MusicNote,
                    label   = "MUSIC",
                    color   = MilitaryGreen,
                    onClick = onPlayMusic,
                    modifier = Modifier.weight(1f),
                )
                TinSoundButton(
                    icon    = Icons.Filled.DirectionsRun,
                    label   = "DANCE",
                    color   = RustOrange,
                    onClick = onDance,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        RivetRow()
    }
}

@Composable
private fun TinSoundButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) color.copy(alpha = 0.25f) else color.copy(alpha = 0.1f))
            .border(
                width = if (isPressed) 2.5.dp else 1.5.dp,
                color = color.copy(alpha = if (isPressed) 0.9f else 0.5f),
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
            Text(
                label,
                color      = color,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 장치 선택 다이얼로그
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
private fun TinDevicePicker(
    devices: List<BluetoothDevice>,
    onSelect: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = SteelMid) {
            Column(modifier = Modifier.padding(0.dp)) {
                // 헤더
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PanelInner)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Bluetooth, null, tint = ActiveAmber, modifier = Modifier.size(18.dp))
                        Text(
                            "SELECT DEVICE",
                            color      = CreamText,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                HorizontalDivider(color = PanelBorder, thickness = 2.dp)

                Column(modifier = Modifier.padding(12.dp)) {
                    if (devices.isEmpty()) {
                        Text(
                            "페어링된 장치 없음\n설정 > 블루투스에서 HC-06을 먼저 페어링하세요.",
                            color    = MutedBrown,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 300.dp),
                        ) {
                            items(devices) { device ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SteelDark)
                                        .border(1.dp, PanelBorder, RoundedCornerShape(4.dp))
                                        .clickable { onSelect(device) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.BluetoothConnected, null, tint = ActiveAmber, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(device.name ?: "Unknown", color = CreamText, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                        Text(device.address, color = MutedBrown, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = onDismiss) {
                            Text("CANCEL", color = MutedBrown, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
