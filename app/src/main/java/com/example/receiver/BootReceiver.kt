package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.audio.VolumeLockService
import com.example.data.SettingsRepository
import com.example.vpn.SultanBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("BootReceiver", "Device boot completed. Checking SULTAN BLOCK restore settings.")
        val settingsRepo = SettingsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            val restoreInternetBlock = settingsRepo.autoRestoreInternetOnBoot.first()
            val restoreVolumeLock = settingsRepo.autoRestoreVolumeOnBoot.first()

            if (restoreInternetBlock) {
                // Check if VPN was prepared
                val prepareIntent = android.net.VpnService.prepare(context)
                if (prepareIntent == null) {
                    val vpnIntent = Intent(context, SultanBlockVpnService::class.java).apply {
                        action = SultanBlockVpnService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(vpnIntent)
                    } else {
                        context.startService(vpnIntent)
                    }
                }
            }

            if (restoreVolumeLock) {
                val volumeIntent = Intent(context, VolumeLockService::class.java).apply {
                    action = VolumeLockService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(volumeIntent)
                } else {
                    context.startService(volumeIntent)
                }
            }
        }
    }
}
