package com.example.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.audio.VolumeLockService
import com.example.certainlock.CertainLockForegroundService
import com.example.certainlock.CertainLockManager
import com.example.data.SettingsRepository
import com.example.vpn.SultanBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

data class TimerState(
    val isActive: Boolean = false,
    val durationMinutes: Long = 0L,
    val startTimestamp: Long = 0L,
    val endTimestamp: Long = 0L,
    val controlInternet: Boolean = true,
    val controlVolume: Boolean = true,
    val controlAppLock: Boolean = true
) {
    fun remainingMillis(): Long {
        if (!isActive) return 0L
        val remaining = endTimestamp - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun formatRemaining(): String {
        val totalSecs = remainingMillis() / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}

class TimerManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val TAG = "TimerManager"
        const val ACTION_TIMER_EXPIRED = "com.example.timer.ACTION_TIMER_EXPIRED"
        private const val ALARM_REQ_CODE = 9988
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    suspend fun loadState() {
        val json = settingsRepository.timerJson.first()
        val parsed = parseJson(json)
        // Check if expired while app was dead
        if (parsed.isActive && parsed.endTimestamp <= System.currentTimeMillis()) {
            _timerState.value = TimerState()
            settingsRepository.saveTimerJson("")
        } else {
            _timerState.value = parsed
        }
    }

    suspend fun startTimer(
        durationMinutes: Long,
        controlInternet: Boolean,
        controlVolume: Boolean,
        controlAppLock: Boolean
    ) {
        val start = System.currentTimeMillis()
        val end = start + (durationMinutes * 60 * 1000)

        val state = TimerState(
            isActive = true,
            durationMinutes = durationMinutes,
            startTimestamp = start,
            endTimestamp = end,
            controlInternet = controlInternet,
            controlVolume = controlVolume,
            controlAppLock = controlAppLock
        )
        _timerState.value = state
        settingsRepository.saveTimerJson(serializeJson(state))

        // Schedule exact alarm
        val intent = Intent(context, TimerReceiver::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, end, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, end, pendingIntent)
        }

        // Arm requested features
        if (controlInternet) {
            val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                this.action = SultanBlockVpnService.ACTION_START
            }
            startServiceCompat(vpnIntent)
        }
        if (controlVolume) {
            val volIntent = Intent(context, VolumeLockService::class.java).apply {
                this.action = VolumeLockService.ACTION_START
            }
            startServiceCompat(volIntent)
        }
        if (controlAppLock) {
            CertainLockManager.updateActiveState(true)
            val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                this.action = CertainLockForegroundService.ACTION_START
            }
            startServiceCompat(clIntent)
        }

        Log.i(TAG, "Started timer for $durationMinutes minutes. Ends at $end")
    }

    suspend fun cancelTimer() {
        val current = _timerState.value
        _timerState.value = TimerState()
        settingsRepository.saveTimerJson("")

        // Cancel AlarmManager
        val intent = Intent(context, TimerReceiver::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
        }

        // Deactivate features controlled by this timer
        if (current.controlInternet) {
            val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                this.action = SultanBlockVpnService.ACTION_STOP
            }
            context.startService(vpnIntent)
        }
        if (current.controlVolume) {
            val volIntent = Intent(context, VolumeLockService::class.java).apply {
                this.action = VolumeLockService.ACTION_STOP
            }
            context.startService(volIntent)
        }
        if (current.controlAppLock) {
            CertainLockManager.updateActiveState(false)
            val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                this.action = CertainLockForegroundService.ACTION_STOP
            }
            context.startService(clIntent)
        }
        Log.i(TAG, "Timer cancelled and associated features disabled.")
    }

    private fun startServiceCompat(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service from timer: ${e.message}")
        }
    }

    private fun parseJson(json: String?): TimerState {
        if (json.isNullOrEmpty()) return TimerState()
        return try {
            val obj = JSONObject(json)
            TimerState(
                isActive = obj.getBoolean("isActive"),
                durationMinutes = obj.getLong("durationMinutes"),
                startTimestamp = obj.getLong("startTimestamp"),
                endTimestamp = obj.getLong("endTimestamp"),
                controlInternet = obj.optBoolean("controlInternet", true),
                controlVolume = obj.optBoolean("controlVolume", true),
                controlAppLock = obj.optBoolean("controlAppLock", true)
            )
        } catch (e: Exception) {
            TimerState()
        }
    }

    private fun serializeJson(state: TimerState): String {
        return JSONObject().apply {
            put("isActive", state.isActive)
            put("durationMinutes", state.durationMinutes)
            put("startTimestamp", state.startTimestamp)
            put("endTimestamp", state.endTimestamp)
            put("controlInternet", state.controlInternet)
            put("controlVolume", state.controlVolume)
            put("controlAppLock", state.controlAppLock)
        }.toString()
    }
}
