package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.audio.VolumeLockService
import com.example.certainlock.CertainLockForegroundService
import com.example.certainlock.CertainLockManager
import com.example.data.SettingsRepository
import com.example.schedule.ScheduleManager
import com.example.timer.TimerManager
import com.example.vpn.SultanBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("BootReceiver", "Device boot completed. Restoring SULTAN BLOCK services and alarms.")
        val settingsRepo = SettingsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            // Restore Schedules
            val scheduleManager = ScheduleManager(context, settingsRepo)
            scheduleManager.loadSchedules()

            // Restore Timer
            val timerManager = TimerManager(context, settingsRepo)
            timerManager.loadState()

            // Restore Certain Lock packages
            val lockedPkgs = settingsRepo.lockedPackages.first()
            CertainLockManager.setLockedPackages(lockedPkgs)
            val isCertainLockActive = settingsRepo.isCertainLockActive.first()
            if (isCertainLockActive) {
                CertainLockManager.updateActiveState(true)
                val clIntent = Intent(context, CertainLockForegroundService::class.java).apply {
                    action = CertainLockForegroundService.ACTION_START
                }
                startServiceCompat(context, clIntent)
            }

            // Restore Internet Block
            val restoreInternetBlock = settingsRepo.autoRestoreInternetOnBoot.first()
            if (restoreInternetBlock) {
                val prepareIntent = android.net.VpnService.prepare(context)
                if (prepareIntent == null) {
                    val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                        action = SultanBlockVpnService.ACTION_START
                    }
                    startServiceCompat(context, vpnIntent)
                }
            }

            // Restore Volume Lock
            val restoreVolumeLock = settingsRepo.autoRestoreVolumeOnBoot.first()
            if (restoreVolumeLock) {
                val volumeIntent = Intent(context, VolumeLockService::class.java).apply {
                    action = VolumeLockService.ACTION_START
                }
                startServiceCompat(context, volumeIntent)
            }
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
            Log.e("BootReceiver", "Failed to start service on boot: ${e.message}")
        }
    }
}
