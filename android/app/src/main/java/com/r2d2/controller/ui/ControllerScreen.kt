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
    onHorn: () -> Unit,
) {
    var showDevicePicker by remember { mutableStateOf(false) }
    val isConnected = btState is BtState.Connected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── 상태 바 ──────────────────────────────────────────────────
        StatusBar(
            btState        = btState,
            onConnectClick = {
                if (isConnected) onDisconnect()
                else showDevicePicker = true
            },
        )

        // ── 헤더 ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "ArduD2 컨트롤러",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }

        HorizontalDivider(color = BorderColor)

        // ── 모드 토글 ────────────────────────────────────────────────
        ModeToggleSection(isManualMode = isManualMode, onToggle = onToggleMode)

        // ── 속도 프리셋 (수동 모드) ──────────────────────────────────
        AnimatedVisibility(visible = isManualMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                SpeedPreset.entries.forEach { preset ->
                    val active = speed == preset
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (active) NeonCyan else BorderColor, CircleShape)
                            .clickable { onSetSpeed(preset) }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = preset.label,
                            color      = if (active) NeonCyan else MutedText,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 24.dp))

        // ── 조이스틱 / 트래킹 상태 ──────────────────────────────────
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
                TrackingStatusView(isTracking = isTracking)
            }
        }

        HorizontalDivider(color = BorderColor)

        // ── 상체 제어 슬라이더 (수동 모드일 때만) ────────────────────
        if (isManualMode) {
            BodyControlSection(
                currentAngle = bodyAngle,
                targetAngle  = targetBodyAngle,
                limitError   = bodyLimitError,
                onSetTarget  = onSetTargetBodyAngle,
                onHome       = onBodyHome,
            )
            HorizontalDivider(color = BorderColor)
        }

        // ── 사운드 버튼 ──────────────────────────────────────────────
        SoundButtonsSection(
            onSayHello  = onSayHello,
            onPlayMusic = onPlayMusic,
            onHorn      = onHorn,
        )
    }

    // ── 장치 선택 다이얼로그 ─────────────────────────────────────────
    if (showDevicePicker) {
        DevicePickerDialog(
            devices   = pairedDevices,
            onSelect  = { device ->
                showDevicePicker = false
                onConnect(device)
            },
            onDismiss = { showDevicePicker = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 상태 바 (웹 버전: Wifi 아이콘 + Plug/Unplug 버튼)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(
    btState: BtState,
    onConnectClick: () -> Unit,
) {
    val isConnected  = btState is BtState.Connected
    val isConnecting = btState is BtState.Connecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulse",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: Wifi 아이콘 + 상태 텍스트
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box {
                Icon(
                    imageVector = if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint     = if (isConnected) NeonGreen else MutedText,
                    modifier = Modifier.size(20.dp),
                )
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = pulseAlpha)),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("상태:", color = MutedText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = when {
                        isConnecting -> "연결 중..."
                        isConnected  -> "연결됨"
                        btState is BtState.Error -> "오류"
                        else         -> "연결 안 됨"
                    },
                    color = if (isConnected) NeonGreen else MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // 오른쪽: 초록 dot (연결시) + 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = pulseAlpha)),
                )
            }

            val btnColor = if (isConnected) DangerRed else NeonGreen
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, btnColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isConnecting) { onConnectClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when {
                        isConnecting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp,
                            )
                            Text("연결 중", color = NeonGreen, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                        isConnected -> {
                            Icon(Icons.Filled.PowerOff, null, tint = DangerRed,
                                modifier = Modifier.size(13.dp))
                            Text("해제", color = DangerRed, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                        else -> {
                            Icon(Icons.Filled.Power, null, tint = NeonGreen,
                                modifier = Modifier.size(13.dp))
                            Text("연결", color = NeonGreen, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 모드 토글 (웹 버전: 슬라이딩 필 버튼 with Gamepad/Eye 아이콘)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModeToggleSection(isManualMode: Boolean, onToggle: () -> Unit) {
    val activeColor = if (isManualMode) NeonCyan else NeonGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "제어 모드",
            color      = MutedText,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )

        // 슬라이딩 필 버튼
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F2937))
                .border(1.dp, BorderColor, CircleShape)
                .clickable { onToggle() },
        ) {
            val halfWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue  = if (isManualMode) 0.dp else halfWidth,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "pillOffset",
            )

            // 슬라이딩 인디케이터 배경
            Box(
                modifier = Modifier
                    .absoluteOffset(x = indicatorOffset + 4.dp, y = 4.dp)
                    .size(halfWidth - 8.dp, 48.dp)
                    .clip(CircleShape)
                    .background(activeColor),
            )

            // 레이블 (인디케이터 위에 오버레이)
            Row(modifier = Modifier.fillMaxSize()) {
                // 수동 (왼쪽)
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint     = if (isManualMode) DarkBg else MutedText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "수동",
                            color      = if (isManualMode) DarkBg else MutedText,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // 자동 (오른쪽)
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            tint     = if (!isManualMode) DarkBg else MutedText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "자동",
                            color      = if (!isManualMode) DarkBg else MutedText,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // 부제목
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text       = if (isManualMode) "수동 제어 모드" else "HuskyLens 자율 추적 모드",
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 13.sp,
            )
            Text(
                text    = if (isManualMode) "조이스틱으로 로봇을 제어합니다" else "감지된 사람을 자동으로 추적합니다",
                color   = MutedText,
                fontSize = 11.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 트래킹 상태 (웹 버전: 카메라 뷰포트 + 스캔라인 애니메이션)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrackingStatusView(isTracking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanLine",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "HuskyLens 추적",
            color      = MutedText,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )

        // 카메라 뷰포트
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    2.dp,
                    if (isTracking) NeonGreen.copy(alpha = 0.5f) else Color(0xFF374151),
                    RoundedCornerShape(12.dp),
                )
                .background(Color(0xFF050810)),
            contentAlignment = Alignment.Center,
        ) {
            // Grid + scan line + corner brackets (Canvas)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 3×3 그리드
                val gridColor = NeonGreen.copy(alpha = 0.1f)
                for (i in 1..2) {
                    drawLine(gridColor, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), 1f)
                    drawLine(gridColor, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
                }

                // 스캔라인
                val scanY = h * scanProgress
                drawLine(NeonGreen.copy(alpha = 0.6f), Offset(0f, scanY), Offset(w, scanY), 2f)
                drawLine(NeonGreen.copy(alpha = 0.15f), Offset(0f, scanY - 6f), Offset(w, scanY - 6f), 8f)

                // 코너 브래킷
                val m = 10f
                val bl = 18f
                val bw = 2f
                val bc = NeonGreen.copy(alpha = 0.7f)
                drawLine(bc, Offset(m, m), Offset(m + bl, m), bw)
                drawLine(bc, Offset(m, m), Offset(m, m + bl), bw)
                drawLine(bc, Offset(w - m, m), Offset(w - m - bl, m), bw)
                drawLine(bc, Offset(w - m, m), Offset(w - m, m + bl), bw)
                drawLine(bc, Offset(m, h - m), Offset(m + bl, h - m), bw)
                drawLine(bc, Offset(m, h - m), Offset(m, h - m - bl), bw)
                drawLine(bc, Offset(w - m, h - m), Offset(w - m - bl, h - m), bw)
                drawLine(bc, Offset(w - m, h - m), Offset(w - m, h - m - bl), bw)
            }

            // 중앙 아이콘
            Icon(
                imageVector = if (isTracking) Icons.Filled.GpsFixed else Icons.Filled.RemoveRedEye,
                contentDescription = null,
                tint     = if (isTracking) NeonGreen.copy(alpha = pulseAlpha) else MutedText,
                modifier = Modifier.size(48.dp),
            )
        }

        // 상태 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTracking) NeonGreen.copy(alpha = pulseAlpha)
                            else MutedText
                        ),
                )
                Text(
                    if (isTracking) "추적 중" else "탐색 중...",
                    color      = if (isTracking) NeonGreen else MutedText,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isTracking) {
                Text(
                    "추적 대상: 사람",
                    color    = MutedText,
                    fontSize = 11.sp,
                )
            }
            Text(
                "HuskyLens AI 카메라로 감지된 사람을 자동으로 추적합니다",
                color     = MutedText,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 상체 슬라이더 (기존 유지)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BodyControlSection(
    currentAngle: Int,
    targetAngle: Int,
    limitError: Boolean,
    onSetTarget: (Int) -> Unit,
    onHome: () -> Unit,
) {
    var sliderValue by remember(targetAngle) { mutableFloatStateOf(targetAngle.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("상체 회전", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onHome() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Home, null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Text("원점 복귀", color = NeonCyan, fontSize = 11.sp)
                }
            }
        }

        AnimatedVisibility(visible = limitError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DangerRed.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("최대 회전각 초과 — 범위 내에서 조작하세요", color = DangerRed, fontSize = 12.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AngleBadge(label = "현재", angle = currentAngle, color = NeonGreen)
            AngleBadge(label = "목표", angle = targetAngle, color = NeonCyan)
        }

        Column {
            Slider(
                value         = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val snapped = ((sliderValue / 15f).toInt() * 15).coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG)
                    sliderValue = snapped.toFloat()
                    onSetTarget(snapped)
                },
                valueRange = (-BODY_MAX_DEG).toFloat()..BODY_MAX_DEG.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor         = NeonCyan,
                    activeTrackColor   = NeonCyan,
                    inactiveTrackColor = Color(0xFF374151),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("-${BODY_MAX_DEG}°", color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("0°",               color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("+${BODY_MAX_DEG}°", color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        val delta = kotlin.math.abs(targetAngle - currentAngle)
        if (delta >= 15) {
            val direction = if (targetAngle > currentAngle) "시계방향 ▶" else "◀ 반시계방향"
            Text(
                text  = "이동 중... $direction  (${delta}° 남음)",
                color = Color(0xFFF59E0B),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun AngleBadge(label: String, angle: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MutedText, fontSize = 10.sp)
        Text(
            text       = "${angle}°",
            color      = color,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 사운드 버튼 (웹 버전: 컬러 네온 버튼 with Hand/Music/Bell 아이콘)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SoundButtonsSection(
    onSayHello: () -> Unit,
    onPlayMusic: () -> Unit,
    onHorn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "사운드",
            color      = MutedText,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier   = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NeonSoundButton(
                icon    = Icons.Filled.PanTool,
                label   = "인사하기",
                color   = NeonCyan,
                onClick = onSayHello,
                modifier = Modifier.weight(1f),
            )
            NeonSoundButton(
                icon    = Icons.Filled.MusicNote,
                label   = "음악 재생",
                color   = NeonGreen,
                onClick = onPlayMusic,
                modifier = Modifier.weight(1f),
            )
            NeonSoundButton(
                icon    = Icons.Filled.Campaign,
                label   = "경적",
                color   = NeonOrange,
                onClick = onHorn,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NeonSoundButton(
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
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (isPressed) 0.2f else 0.1f))
            .border(2.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint     = color,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                label,
                color      = color,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 장치 선택 다이얼로그
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
private fun DevicePickerDialog(
    devices: List<BluetoothDevice>,
    onSelect: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Icon(Icons.Filled.Bluetooth, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Text("페어링된 장치", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (devices.isEmpty()) {
                    Text(
                        text  = "페어링된 장치가 없습니다.\n설정 > 블루투스에서 HC-06을 먼저 페어링하세요.",
                        color = MutedText,
                        fontSize = 14.sp,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(devices) { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelect(device) }
                                    .background(Color(0xFF1F2937))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.BluetoothConnected,
                                    null,
                                    tint     = NeonCyan,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text       = device.name ?: "알 수 없음",
                                        color      = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp,
                                    )
                                    Text(
                                        text     = device.address,
                                        color    = MutedText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("취소", color = MutedText)
                }
            }
        }
    }
}
