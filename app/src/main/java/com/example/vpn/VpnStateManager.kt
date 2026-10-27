package com.example.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnStateManager {
    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _blockedPackets = MutableStateFlow(0L)
    val blockedPackets: StateFlow<Long> = _blockedPackets.asStateFlow()

    private val _blockedBytes = MutableStateFlow(0L)
    val blockedBytes: StateFlow<Long> = _blockedBytes.asStateFlow()

    private val _vpnStartTime = MutableStateFlow<Long?>(null)
    val vpnStartTime: StateFlow<Long?> = _vpnStartTime.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun updateActive(active: Boolean) {
        _isVpnActive.value = active
        if (active) {
            _vpnStartTime.value = System.currentTimeMillis()
            _lastError.value = null
        } else {
            _vpnStartTime.value = null
        }
    }

    fun incrementBlocked(packets: Long = 1, bytes: Long = 0) {
        _blockedPackets.value += packets
        _blockedBytes.value += bytes
    }

    fun resetCounters() {
        _blockedPackets.value = 0L
        _blockedBytes.value = 0L
    }

    fun setError(error: String?) {
        _lastError.value = error
    }
}
