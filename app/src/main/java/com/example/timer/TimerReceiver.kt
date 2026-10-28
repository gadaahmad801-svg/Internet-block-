package com.example.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.SultanBlockApp
import com.example.audio.VolumeLockService
import com.example.certainlock.CertainLockForegroundService
import com.example.certainlock.CertainLockManager
import com.example.data.SettingsRepository
import com.example.vpn.SultanBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

class TimerReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "TimerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TimerManager.ACTION_TIMER_EXPIRED) return

        Log.i(TAG, "Timer expired broadcast received. Releasing timer-controlled blocks.")

        val settingsRepo = SettingsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            val json = settingsRepo.timerJson.first()
            if (json.isNullOrEmpty()) return@launch

            val (ctrlInternet, ctrlVolume, ctrlAppLock) = try {
                val obj = JSONObject(json)
                Triple(
                    obj.optBoolean("controlInternet", true),
                    obj.optBoolean("controlVolume", true),
                    obj.optBoolean("controlAppLock", true)
                )
            } catch (e: Exception) {
                Triple(true, true, true)
            }

            // Clear saved timer state
            settingsRepo.saveTimerJson("")

            if (ctrlInternet) {
                val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                    this.action = SultanBlockVpnService.ACTION_STOP
                }
                context.startService(vpnIntent)
            }

            if (ctrlVolume) {
                val volIntent = Intent(context, VolumeLockService::class.java).apply {
                    this.action = VolumeLockService.ACTION_STOP
                }
                context.startService(volIntent)
            }

            if (ctrlAppLock) {
                CertainLockManager.updateActiveState(false)
                val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                    this.action = CertainLockForegroundService.ACTION_STOP
                }
                context.startService(clIntent)
            }

            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        try {
            val notification = NotificationCompat.Builder(context, SultanBlockApp.SCHEDULE_TIMER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Timer Protection Expired")
                .setContentText("The scheduled duration has ended. Protected features have been released.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(9911, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }
}
