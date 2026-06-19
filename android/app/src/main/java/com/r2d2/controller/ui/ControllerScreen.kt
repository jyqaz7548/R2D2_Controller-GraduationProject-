package com.r2d2.controller.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    // 상체
    bodyAngle: Int,
    targetBodyAngle: Int,
    bodyLimitError: Boolean,
    // 콜백
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
                    .background(NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) { Text("🤖", fontSize = 20.sp) }
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
        ModeToggleRow(isManualMode = isManualMode, onToggle = onToggleMode)

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
                            color      = if (active) NeonCyan else Color(0xFF6B7280),
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
                .height(320.dp),
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

        // ── 상체 제어 슬라이더 ───────────────────────────────────────
        BodyControlSection(
            currentAngle    = bodyAngle,
            targetAngle     = targetBodyAngle,
            limitError      = bodyLimitError,
            onSetTarget     = onSetTargetBodyAngle,
            onHome          = onBodyHome,
        )

        HorizontalDivider(color = BorderColor)

        // ── 사운드 버튼 ──────────────────────────────────────────────
        SoundButtons(
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
// 상태 바
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(
    btState: BtState,
    onConnectClick: () -> Unit,
) {
    val isConnected  = btState is BtState.Connected
    val isConnecting = btState is BtState.Connecting

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isConnected  -> NeonGreen
                        isConnecting -> Color(0xFFF59E0B)
                        else         -> Color(0xFF6B7280)
                    }
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                isConnected  -> "연결됨"
                isConnecting -> "연결 중..."
                btState is BtState.Error -> "오류: ${btState.message}"
                else         -> "연결 안됨"
            },
            color    = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onConnectClick,
            enabled = !isConnecting,
            colors  = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFF374151) else NeonCyan,
                contentColor   = if (isConnected) Color.White else DarkBg,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text     = if (isConnected) "연결 해제" else "연결",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 모드 토글
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModeToggleRow(isManualMode: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text  = if (isManualMode) "수동 모드" else "자동 모드",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
            Text(
                text  = if (isManualMode) "조이스틱으로 직접 제어" else "HuskyLens 자동 추적",
                color = Color(0xFF6B7280), fontSize = 12.sp,
            )
        }
        Switch(
            checked         = !isManualMode,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor   = DarkBg,
                checkedTrackColor   = NeonCyan,
                uncheckedThumbColor = Color(0xFF6B7280),
                uncheckedTrackColor = Color(0xFF374151),
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 트래킹 상태
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrackingStatusView(isTracking: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = if (isTracking) "🎯" else "👁", fontSize = 48.sp)
        Text(
            text  = if (isTracking) "타겟 추적 중" else "타겟 탐색 중...",
            color = if (isTracking) NeonCyan else Color(0xFF6B7280),
            fontWeight = FontWeight.Bold, fontSize = 16.sp,
        )
        Text(text = "자동 모드 활성", color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 상체 슬라이더
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BodyControlSection(
    currentAngle: Int,
    targetAngle: Int,
    limitError: Boolean,
    onSetTarget: (Int) -> Unit,
    onHome: () -> Unit,
) {
    // 슬라이더가 드래그 중일 때 사용하는 임시 값
    var sliderValue by remember(targetAngle) { mutableFloatStateOf(targetAngle.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 헤더 ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "상체 회전",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
            // 원점 복귀 버튼
            OutlinedButton(
                onClick = onHome,
                border  = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.6f))
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
            ) {
                Text("원점 복귀", color = NeonCyan, fontSize = 11.sp)
            }
        }

        // ── 에러 경고 ─────────────────────────────────────────────
        AnimatedVisibility(visible = limitError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DangerRed.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⚠", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text("최대 회전각 초과 — 범위 내에서 조작하세요", color = DangerRed, fontSize = 12.sp)
            }
        }

        // ── 각도 표시 ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AngleBadge(label = "현재", angle = currentAngle, color = NeonGreen)
            AngleBadge(label = "목표", angle = targetAngle, color = NeonCyan)
        }

        // ── 슬라이더 ──────────────────────────────────────────────
        Column {
            Slider(
                value         = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    // 손을 떼면 15도 단위로 스냅해서 ViewModel에 전달
                    val snapped = ((sliderValue / 15f).toInt() * 15).coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG)
                    sliderValue = snapped.toFloat()
                    onSetTarget(snapped)
                },
                valueRange = (-BODY_MAX_DEG).toFloat()..BODY_MAX_DEG.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor            = NeonCyan,
                    activeTrackColor      = NeonCyan,
                    inactiveTrackColor    = Color(0xFF374151),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            // 범위 레이블
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("-${BODY_MAX_DEG}°", color = Color(0xFF6B7280), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("0°",               color = Color(0xFF6B7280), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("+${BODY_MAX_DEG}°", color = Color(0xFF6B7280), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // ── 진행 상태 ─────────────────────────────────────────────
        val delta = Math.abs(targetAngle - currentAngle)
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
        Text(text = label, color = Color(0xFF6B7280), fontSize = 10.sp)
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
// 사운드 버튼
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SoundButtons(
    onSayHello: () -> Unit,
    onPlayMusic: () -> Unit,
    onHorn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            Triple("👋", "인사",  onSayHello),
            Triple("🎵", "음악",  onPlayMusic),
            Triple("📢", "경적",  onHorn),
        ).forEach { (emoji, label, onClick) ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 20.sp)
                    Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
                }
            }
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
                Text(
                    text = "페어링된 장치", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                if (devices.isEmpty()) {
                    Text(
                        text  = "페어링된 장치가 없습니다.\n설정 > 블루투스에서 HC-06을 먼저 페어링하세요.",
                        color = Color(0xFF6B7280), fontSize = 14.sp,
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
                                Text("🔵", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text  = device.name ?: "알 수 없음",
                                        color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    )
                                    Text(
                                        text  = device.address,
                                        color = Color(0xFF6B7280), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("취소", color = Color(0xFF6B7280))
                }
            }
        }
    }
}
