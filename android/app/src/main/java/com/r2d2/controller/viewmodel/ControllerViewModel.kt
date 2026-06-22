package com.r2d2.controller.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.r2d2.controller.RobotCommands
import com.r2d2.controller.bluetooth.BluetoothService
import com.r2d2.controller.bluetooth.BtState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SpeedPreset(val label: String, val mult: Float) {
    LOW("LOW", 0.4f),
    MED("MED", 0.7f),
    HIGH("HIGH", 1.0f)
}

const val BODY_STEP_DEG = 15   // 슬라이더 스냅 단위 (G 명령 전송 전 반올림)
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

    // ── 기타 ───────────────────────────────────────────────────────────
    @Volatile private var lastCmd = ""                  // IO/Main 교차 읽기 안전
    private val btMutex = Mutex()                       // BT 쓰기 직렬화용 뮤텍스

    // ─────────────────────────────────────────────────────────────────
    init {
        // Arduino → App 수신 파싱
        viewModelScope.launch {
            bluetoothService.dataFlow.collect { line ->
                when {
                    line == "TRACKING:1" -> _isTracking.value = true
                    line == "TRACKING:0" -> _isTracking.value = false

                    // v6: Arduino가 도달 시 + 이동 중 500ms마다 BODY:{angle} 전송
                    // 앱은 그냥 현재 각도만 업데이트 (추가 명령 전송 불필요)
                    line.startsWith("BODY:") -> {
                        val angle = line.removePrefix("BODY:").trim().toIntOrNull() ?: return@collect
                        _bodyAngle.value = angle
                    }

                    line == "ESTOP:OK" -> {
                        // 비상정지 확인 — ViewModel 상태는 emergencyStop()에서 이미 처리됨
                    }
                }
            }
        }

        // 워치독 keepalive — 조이스틱 고정 중 400ms마다 현재 명령 재전송
        // (앱이 중복 명령을 차단하지만 아두이노 워치독(1000ms)보다 짧게 유지)
        viewModelScope.launch(Dispatchers.IO) {
            val motionPrefixes = setOf('F', 'B', 'L', 'R')
            while (true) {
                delay(400)
                val cmd = lastCmd
                if (_isManualMode.value && cmd.isNotEmpty() && cmd[0] in motionPrefixes) {
                    btMutex.withLock { bluetoothService.send(cmd) }  // 뮤텍스로 직렬화
                }
            }
        }

        // 연결 상태 감시
        viewModelScope.launch {
            var prev: BtState = BtState.Disconnected
            bluetoothService.state.collect { s ->
                if (prev !is BtState.Connected && s is BtState.Connected) {
                    send(RobotCommands.manualMode())   // 수동 모드 진입
                    send(RobotCommands.connected())    // 연결 확인음 (0011.mp3)
                }
                if (s is BtState.Disconnected) {
                    _isTracking.value = false
                    _bodyAngle.value = 0
                    _targetBodyAngle.value = 0
                    lastCmd = ""   // 재연결 시 keepalive 자동 주행 방지
                }
                prev = s
            }
        }
    }

    // ─── 연결 ────────────────────────────────────────────────────────
    fun connect(device: BluetoothDevice) = bluetoothService.connect(device)

    /** 연결 해제 — 종료음(Z)을 먼저 전송 후 1.5초 뒤 BT 해제 */
    fun disconnect() {
        viewModelScope.launch {
            send(RobotCommands.shutdown())   // 0012.mp3 트리거
            delay(1500)                       // 아두이노가 파일 재생 시작할 시간
            bluetoothService.disconnect()
        }
    }

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

    // ─── 비상정지 ────────────────────────────────────────────────────
    fun emergencyStop() {
        send(RobotCommands.emergencyStop())
        _isManualMode.value = true
        _isTracking.value   = false
        lastCmd = ""
    }

    // ─── 사운드 ──────────────────────────────────────────────────────
    fun sayHello()  = send(RobotCommands.sayHello())
    fun playMusic() = send(RobotCommands.playMusic())
    fun dance()     = send(RobotCommands.dance())

    // ─── 상체 제어 ───────────────────────────────────────────────────

    /**
     * 슬라이더가 목표 각도를 설정 (15도 단위 스냅)
     * v6: G{angle} 명령 한 번으로 Arduino에 절대 각도 전달
     * Arduino가 내부적으로 회전 처리 후 BODY:{angle} 응답
     */
    fun setTargetBodyAngle(angleDeg: Int) {
        val clamped = angleDeg.coerceIn(-BODY_MAX_DEG, BODY_MAX_DEG)
        _targetBodyAngle.value = clamped
        send(RobotCommands.bodyGoto(clamped))
    }

    /** 상체 원점 복귀 (H 명령) */
    fun bodyHome() {
        _targetBodyAngle.value = 0
        send(RobotCommands.bodyHome())
    }

    /** 현재 위치를 0도로 수동 영점 초기화 (I 명령) — 로봇을 정면으로 맞춘 후 호출 */
    fun bodyZeroReset() {
        _bodyAngle.value = 0
        _targetBodyAngle.value = 0
        send(RobotCommands.bodyZeroReset())
    }

    // ─── 내부 ────────────────────────────────────────────────────────
    val pairedDevices get() = bluetoothService.pairedDevices
    val isBluetoothEnabled get() = bluetoothService.isBluetoothEnabled
    val isAdapterAvailable get() = bluetoothService.isAdapterAvailable

    private fun send(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            btMutex.withLock { bluetoothService.send(cmd) }
        }
    }

    override fun onCleared() {
        bluetoothService.destroy()
        super.onCleared()
    }
}
