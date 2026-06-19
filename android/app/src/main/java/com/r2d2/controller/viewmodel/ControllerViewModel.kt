package com.r2d2.controller.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.r2d2.controller.RobotCommands
import com.r2d2.controller.bluetooth.BluetoothService
import com.r2d2.controller.bluetooth.BtState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SpeedPreset(val label: String, val mult: Float) {
    LOW("LOW", 0.4f),
    MED("MED", 0.7f),
    HIGH("HIGH", 1.0f)
}

const val BODY_STEP_DEG = 15   // Arduino는 T/U 한 번에 15도씩 이동
const val BODY_MAX_DEG  = 350  // Arduino 각도 제한

@SuppressLint("MissingPermission")
class ControllerViewModel(app: Application) : AndroidViewModel(app) {

    val bluetoothService = BluetoothService(app)
    val btState = bluetoothService.state

    // ── 하체 ───────────────────────────────────────────────────────────
    private val _isManualMode = MutableStateFlow(true)
    val isManualMode = _isManualMode.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _speed = MutableStateFlow(SpeedPreset.MED)
    val speed = _speed.asStateFlow()

    // ── 상체 ───────────────────────────────────────────────────────────
    /** Arduino가 BODY:{angle} 로 확인해준 현재 각도 */
    private val _bodyAngle = MutableStateFlow(0)
    val bodyAngle = _bodyAngle.asStateFlow()

    /** 슬라이더가 설정한 목표 각도 (15도 단위 스냅) */
    private val _targetBodyAngle = MutableStateFlow(0)
    val targetBodyAngle = _targetBodyAngle.asStateFlow()

    /** E:BODY_LIMIT 수신 시 true */
    private val _bodyLimitError = MutableStateFlow(false)
    val bodyLimitError = _bodyLimitError.asStateFlow()

    /** Arduino가 상체를 이동 중인지 (BODY:{angle} 응답 전까지 true) */
    private var bodyMoving = false

    // ── 기타 ───────────────────────────────────────────────────────────
    private var lastCmd = ""
    private var limitErrorJob: Job? = null

    // ─────────────────────────────────────────────────────────────────
    init {
        // Arduino → App 수신 파싱
        viewModelScope.launch {
            bluetoothService.dataFlow.collect { line ->
                when {
                    line == "TRACKING:1" -> _isTracking.value = true
                    line == "TRACKING:0" -> _isTracking.value = false

                    line.startsWith("BODY:") -> {
                        val angle = line.removePrefix("BODY:").trim().toIntOrNull() ?: return@collect
                        _bodyAngle.value = angle
                        bodyMoving = false
                        // 목표 각도에 아직 못 도달했으면 다음 명령 전송
                        sendNextBodyStep()
                    }

                    line == "E:BODY_LIMIT" -> {
                        _bodyLimitError.value = true
                        // 현재 각도를 목표로 리셋 (더 이상 명령 안 보냄)
                        _targetBodyAngle.value = _bodyAngle.value
                        bodyMoving = false
                        // 3초 후 에러 표시 해제
                        limitErrorJob?.cancel()
                        limitErrorJob = viewModelScope.launch {
                            delay(3000)
                            _bodyLimitError.value = false
                        }
                    }
                }
            }
        }

        // 연결 상태 감시
        viewModelScope.launch {
            var prev: BtState = BtState.Disconnected
            bluetoothService.state.collect { s ->
                if (prev !is BtState.Connected && s is BtState.Connected) {
                    send(RobotCommands.manualMode())
                }
                if (s is BtState.Disconnected) {
                    _isTracking.value = false
                    _bodyAngle.value = 0
                    _targetBodyAngle.value = 0
                    bodyMoving = false
                }
                prev = s
            }
        }
    }

    // ─── 연결 ────────────────────────────────────────────────────────
    fun connect(device: BluetoothDevice) = bluetoothService.connect(device)
    fun disconnect() = bluetoothService.disconnect()

    // ─── 조이스틱 ────────────────────────────────────────────────────
    fun sendJoystick(x: Float, y: Float) {
        val cmd = RobotCommands.fromJoystick(x, y, _speed.value.mult)
        if (cmd != lastCmd) {
            lastCmd = cmd
            send(cmd)
        }
    }

    fun sendStop() {
        lastCmd = ""
        send(RobotCommands.stop())
    }

    // ─── 모드 전환 ───────────────────────────────────────────────────
    fun toggleMode() {
        val next = !_isManualMode.value
        _isManualMode.value = next
        if (next) {
            _isTracking.value = false
            send(RobotCommands.manualMode())
        } else {
            send(RobotCommands.autoMode())
        }
    }

    // ─── 속도 ────────────────────────────────────────────────────────
    fun setSpeed(preset: SpeedPreset) { _speed.value = preset }

    // ─── 사운드 ──────────────────────────────────────────────────────
    fun sayHello()  = send(RobotCommands.sayHello())
    fun playMusic() = send(RobotCommands.playMusic())
    fun horn()      = send(RobotCommands.horn())

    // ─── 상체 제어 ───────────────────────────────────────────────────

    /**
     * 슬라이더가 목표 각도를 설정 (15도 단위 스냅)
     * 설정 즉시 자동으로 T/U 명령 전송 시작
     */
    fun setTargetBodyAngle(angleDeg: Int) {
        val snapped = ((angleDeg / BODY_STEP_DEG) * BODY_STEP_DEG)
            .coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG)
        _targetBodyAngle.value = snapped
        sendNextBodyStep()
    }

    /** 상체 원점 복귀 (H 명령) */
    fun bodyHome() {
        _targetBodyAngle.value = 0
        bodyMoving = true
        send(RobotCommands.bodyHome())
    }

    /**
     * 현재각도 → 목표각도로 한 스텝씩 이동
     * Arduino가 BODY:{angle} 응답 올 때마다 호출됨
     */
    private fun sendNextBodyStep() {
        if (bodyMoving) return
        val delta = _targetBodyAngle.value - _bodyAngle.value
        if (Math.abs(delta) < BODY_STEP_DEG / 2) return   // 이미 도달
        bodyMoving = true
        send(if (delta > 0) RobotCommands.bodyRight() else RobotCommands.bodyLeft())
    }

    // ─── 내부 ────────────────────────────────────────────────────────
    val pairedDevices get() = bluetoothService.pairedDevices
    val isBluetoothEnabled get() = bluetoothService.isBluetoothEnabled
    val isAdapterAvailable get() = bluetoothService.isAdapterAvailable

    private fun send(cmd: String) {
        viewModelScope.launch { bluetoothService.send(cmd) }
    }

    override fun onCleared() {
        bluetoothService.destroy()
        super.onCleared()
    }
}
