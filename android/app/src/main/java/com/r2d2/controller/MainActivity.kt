package com.r2d2.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.r2d2.controller.ui.ControllerScreen
import com.r2d2.controller.ui.theme.R2D2Theme
import com.r2d2.controller.viewmodel.ControllerViewModel

class MainActivity : ComponentActivity() {

    private val vm: ControllerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 권한 결과는 UI 레이어에서 pairedDevices로 처리됨 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBluetoothPermissions()

        setContent {
            R2D2Theme {
                val btState          by vm.btState.collectAsState()
                val isManualMode     by vm.isManualMode.collectAsState()
                val isTracking       by vm.isTracking.collectAsState()
                val speed            by vm.speed.collectAsState()
                val bodyAngle        by vm.bodyAngle.collectAsState()
                val targetBodyAngle  by vm.targetBodyAngle.collectAsState()

                ControllerScreen(
                    btState              = btState,
                    isManualMode         = isManualMode,
                    isTracking           = isTracking,
                    speed                = speed,
                    pairedDevices        = vm.pairedDevices,
                    bodyAngle            = bodyAngle,
                    targetBodyAngle      = targetBodyAngle,
                    onConnect            = vm::connect,
                    onDisconnect         = vm::disconnect,
                    onJoystickMove       = vm::sendJoystick,
                    onJoystickRelease    = vm::sendStop,
                    onToggleMode         = vm::toggleMode,
                    onSetSpeed           = vm::setSpeed,
                    onSetTargetBodyAngle = vm::setTargetBodyAngle,
                    onBodyHome           = vm::bodyHome,
                    onEmergencyStop      = vm::emergencyStop,
                    onSayHello           = vm::sayHello,
                    onPlayMusic          = vm::playMusic,
                    onDance              = vm::dance,
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            )
        }
        val missing = needed.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }
}
