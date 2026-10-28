package com.example.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ScheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val scheduleId = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_ID)

        Log.i(TAG, "Schedule event received: $action for ID $scheduleId")

        val settingsRepo = SettingsRepository(context)
        val scheduleManager = ScheduleManager(context, settingsRepo)

        CoroutineScope(Dispatchers.IO).launch {
            scheduleManager.loadSchedules()
            val schedule = scheduleManager.schedules.value.find { it.id == scheduleId } ?: return@launch
            if (!schedule.isEnabled) return@launch

            when (action) {
                ScheduleManager.ACTION_SCHEDULE_START -> {
                    Log.i(TAG, "Activating schedule: ${schedule.title}")
                    if (schedule.controlInternet) {
                        val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                            this.action = SultanBlockVpnService.ACTION_START
                        }
                        startServiceCompat(context, vpnIntent)
                    }
                    if (schedule.controlVolume) {
                        val volIntent = Intent(context, VolumeLockService::class.java).apply {
                            this.action = VolumeLockService.ACTION_START
                        }
                        startServiceCompat(context, volIntent)
                    }
                    if (schedule.controlAppLock) {
                        CertainLockManager.updateActiveState(true)
                        val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                            this.action = CertainLockForegroundService.ACTION_START
                        }
                        startServiceCompat(context, clIntent)
                    }

                    showNotification(
                        context,
                        "Schedule Active: ${schedule.title}",
                        "Protected period has started. Configured blocks are armed."
                    )
                }

                ScheduleManager.ACTION_SCHEDULE_END -> {
                    Log.i(TAG, "Deactivating schedule: ${schedule.title}")
                    if (schedule.controlInternet) {
                        val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                            this.action = SultanBlockVpnService.ACTION_STOP
                        }
                        context.startService(vpnIntent)
                    }
                    if (schedule.controlVolume) {
                        val volIntent = Intent(context, VolumeLockService::class.java).apply {
                            this.action = VolumeLockService.ACTION_STOP
                        }
                        context.startService(volIntent)
                    }
                    if (schedule.controlAppLock) {
                        CertainLockManager.updateActiveState(false)
                        val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                            this.action = CertainLockForegroundService.ACTION_STOP
                        }
                        context.startService(clIntent)
                    }

                    showNotification(
                        context,
                        "Schedule Ended: ${schedule.title}",
                        "Protected period has completed. Blocks released."
                    )
                }
            }

            // Reschedule next repeating trigger
            scheduleManager.scheduleAlarm(schedule)
        }
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service from schedule: ${e.message}")
        }
    }

    private fun showNotification(context: Context, title: String, content: String) {
        try {
            val notification = NotificationCompat.Builder(context, SultanBlockApp.SCHEDULE_TIMER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }
}
