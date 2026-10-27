package com.example.audio

import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioStreamInfo(
    val streamType: Int,
    val name: String,
    val currentVolume: Int,
    val maxVolume: Int,
    val isLocked: Boolean
)

object VolumeStateManager {
    private val _isLockActive = MutableStateFlow(false)
    val isLockActive: StateFlow<Boolean> = _isLockActive.asStateFlow()

    private val _suppressionCount = MutableStateFlow(0L)
    val suppressionCount: StateFlow<Long> = _suppressionCount.asStateFlow()

    private val _lastSuppressedTime = MutableStateFlow<Long?>(null)
    val lastSuppressedTime: StateFlow<Long?> = _lastSuppressedTime.asStateFlow()

    private val _streams = MutableStateFlow<List<AudioStreamInfo>>(emptyList())
    val streams: StateFlow<List<AudioStreamInfo>> = _streams.asStateFlow()

    private val _lockMediaOnly = MutableStateFlow(false)
    val lockMediaOnly: StateFlow<Boolean> = _lockMediaOnly.asStateFlow()

    fun updateLockActive(active: Boolean) {
        _isLockActive.value = active
    }

    fun incrementSuppression() {
        _suppressionCount.value += 1
        _lastSuppressedTime.value = System.currentTimeMillis()
    }

    fun resetSuppression() {
        _suppressionCount.value = 0L
    }

    fun updateStreams(streamList: List<AudioStreamInfo>) {
        _streams.value = streamList
    }

    fun setLockMediaOnly(mediaOnly: Boolean) {
        _lockMediaOnly.value = mediaOnly
    }
}
