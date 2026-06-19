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

@SuppressLint("MissingPermission")
class ControllerViewModel(app: Application) : AndroidViewModel(app) {

    val bluetoothService = BluetoothService(app)

    val btState = bluetoothService.state

    private val _isManualMode = MutableStateFlow(true)
    val isManualMode = _isManualMode.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _isObstacle = MutableStateFlow(false)
    val isObstacle = _isObstacle.asStateFlow()

    private val _speed = MutableStateFlow(SpeedPreset.MED)
    val speed = _speed.asStateFlow()

    private var lastCmd = ""
    private var obstacleJob: Job? = null

    init {
        // Arduino → App 수신 파싱
        viewModelScope.launch {
            bluetoothService.dataFlow.collect { line ->
                when (line) {
                    "TRACKING:1" -> _isTracking.value = true
                    "TRACKING:0" -> _isTracking.value = false
                    "OBSTACLE"   -> {
                        _isObstacle.value = true
                        obstacleJob?.cancel()
                        obstacleJob = viewModelScope.launch {
                            delay(2000)
                            _isObstacle.value = false
                        }
                    }
                }
            }
        }

        // 연결 상태 감시
        viewModelScope.launch {
            var prev: BtState = BtState.Disconnected
            bluetoothService.state.collect { s ->
                // 연결 직후 → 현재 모드 동기화
                if (prev !is BtState.Connected && s is BtState.Connected) {
                    send(RobotCommands.manualMode())
                }
                // 연결 끊김 → UI 초기화
                if (s is BtState.Disconnected) {
                    _isTracking.value = false
                    _isObstacle.value = false
                }
                prev = s
            }
        }
    }

    // ─── 연결 / 해제 ─────────────────────────────────────────────────────
    fun connect(device: BluetoothDevice) = bluetoothService.connect(device)
    fun disconnect() = bluetoothService.disconnect()

    // ─── 조이스틱 ────────────────────────────────────────────────────────
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

    // ─── 모드 전환 ───────────────────────────────────────────────────────
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

    // ─── 속도 ────────────────────────────────────────────────────────────
    fun setSpeed(preset: SpeedPreset) { _speed.value = preset }

    // ─── 사운드 ──────────────────────────────────────────────────────────
    fun sayHello()  = send(RobotCommands.sayHello())
    fun playMusic() = send(RobotCommands.playMusic())
    fun horn()      = send(RobotCommands.horn())

    // ─── 편의 ────────────────────────────────────────────────────────────
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
