package com.r2d2.controller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

// HC-06 블루투스 시리얼(SPP) 표준 UUID
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

sealed class BtState {
    object Disconnected : BtState()
    object Connecting   : BtState()
    object Connected    : BtState()
    data class Error(val message: String) : BtState()
}

@SuppressLint("MissingPermission")
class BluetoothService(context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var socket: BluetoothSocket? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─── 외부 관찰 가능한 상태 ───────────────────────────────────────────
    private val _state = MutableStateFlow<BtState>(BtState.Disconnected)
    val state = _state.asStateFlow()

    private val _dataFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val dataFlow = _dataFlow.asSharedFlow()

    // ─── 페어링된 장치 목록 (HC-06 포함) ─────────────────────────────────
    val pairedDevices: List<BluetoothDevice>
        get() = adapter?.bondedDevices?.toList() ?: emptyList()

    val isAdapterAvailable: Boolean get() = adapter != null
    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    // ─── 연결 ────────────────────────────────────────────────────────────
    fun connect(device: BluetoothDevice) {
        if (_state.value is BtState.Connecting || _state.value is BtState.Connected) return

        scope.launch {
            _state.value = BtState.Connecting
            try {
                adapter?.cancelDiscovery()
                val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                socket = sock
                _state.value = BtState.Connected
                startReadLoop(sock)
            } catch (e: IOException) {
                socket = null
                _state.value = BtState.Error(e.message ?: "연결 실패")
            }
        }
    }

    // ─── 연결 해제 ───────────────────────────────────────────────────────
    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        _state.value = BtState.Disconnected
    }

    // ─── 명령 전송 ───────────────────────────────────────────────────────
    fun send(command: String): Boolean {
        val s = socket ?: return false
        return try {
            s.outputStream.write((command + "\n").toByteArray(Charsets.UTF_8))
            s.outputStream.flush()
            true
        } catch (e: IOException) {
            handleUnexpectedDisconnect()
            false
        }
    }

    // ─── 수신 루프 ───────────────────────────────────────────────────────
    private fun startReadLoop(sock: BluetoothSocket) {
        readJob = scope.launch {
            val buf = StringBuilder()
            val bytes = ByteArray(256)
            try {
                while (isActive) {
                    val n = sock.inputStream.read(bytes)
                    if (n > 0) {
                        buf.append(String(bytes, 0, n, Charsets.UTF_8))
                        while (buf.contains('\n')) {
                            val idx = buf.indexOf('\n')
                            val line = buf.substring(0, idx).trim()
                            buf.delete(0, idx + 1)
                            if (line.isNotEmpty()) _dataFlow.emit(line)
                        }
                    }
                }
            } catch (e: IOException) {
                if (isActive) handleUnexpectedDisconnect()
            }
        }
    }

    private fun handleUnexpectedDisconnect() {
        scope.launch {
            readJob?.cancel()
            try { socket?.close() } catch (_: IOException) {}
            socket = null
            _state.value = BtState.Disconnected
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
