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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.r2d2.controller.viewmodel.SpeedPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    btState: BtState,
    isManualMode: Boolean,
    isTracking: Boolean,
    isObstacle: Boolean,
    speed: SpeedPreset,
    pairedDevices: List<BluetoothDevice>,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onJoystickRelease: () -> Unit,
    onToggleMode: () -> Unit,
    onSetSpeed: (SpeedPreset) -> Unit,
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
            .navigationBarsPadding(),
    ) {

        // ── 상태 바 ──────────────────────────────────────────────────────
        StatusBar(
            btState       = btState,
            isObstacle    = isObstacle,
            onConnectClick = {
                if (isConnected) onDisconnect()
                else showDevicePicker = true
            },
        )

        // ── 헤더 ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🤖", fontSize = 20.sp)
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

        // ── 모드 토글 ────────────────────────────────────────────────────
        ModeToggleRow(
            isManualMode = isManualMode,
            onToggle     = onToggleMode,
        )

        // ── 속도 프리셋 (수동 모드) ──────────────────────────────────────
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
                            .border(
                                width = 1.dp,
                                color = if (active) NeonCyan else BorderColor,
                                shape = CircleShape,
                            )
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

        // ── 조이스틱 / 트래킹 상태 ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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

        // ── 사운드 버튼 ──────────────────────────────────────────────────
        SoundButtons(
            onSayHello  = onSayHello,
            onPlayMusic = onPlayMusic,
            onHorn      = onHorn,
        )
    }

    // ── 장치 선택 다이얼로그 ─────────────────────────────────────────────
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
    isObstacle: Boolean,
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
        // 상태 도트
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

        // 장애물 경고
        AnimatedVisibility(visible = isObstacle) {
            Text(
                text      = "⚠ 장애물",
                color     = Color(0xFFEF4444),
                fontSize  = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier  = Modifier.padding(end = 12.dp),
            )
        }

        // 연결/해제 버튼
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text  = if (isManualMode) "조이스틱으로 직접 제어" else "HuskyLens 자동 추적",
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
            )
        }
        Switch(
            checked        = !isManualMode,
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
        Text(
            text  = if (isTracking) "🎯" else "👁",
            fontSize = 48.sp,
        )
        Text(
            text  = if (isTracking) "타겟 추적 중" else "타겟 탐색 중...",
            color = if (isTracking) NeonCyan else Color(0xFF6B7280),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text  = "자동 모드 활성",
            color = NeonGreen,
            fontSize = 12.sp,
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
            Triple("👋", "인사", onSayHello),
            Triple("🎵", "음악", onPlayMusic),
            Triple("📢", "경적", onHorn),
        ).forEach { (emoji, label, onClick) ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text       = "페어링된 장치",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    modifier   = Modifier.padding(bottom = 12.dp),
                )
                if (devices.isEmpty()) {
                    Text(
                        text     = "페어링된 장치가 없습니다.\n설정 > 블루투스에서 HC-06을 먼저 페어링하세요.",
                        color    = Color(0xFF6B7280),
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
                                Text("🔵", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text  = device.name ?: "알 수 없음",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text  = device.address,
                                        color = Color(0xFF6B7280),
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
                    Text("취소", color = Color(0xFF6B7280))
                }
            }
        }
    }
}

