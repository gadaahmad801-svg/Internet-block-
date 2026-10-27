package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SultanBlockApp : Application() {

    companion object {
        const val VPN_CHANNEL_ID = "sultan_block_vpn_channel"
        const val VOLUME_CHANNEL_ID = "sultan_block_volume_channel"
        lateinit var instance: SultanBlockApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val vpnChannel = NotificationChannel(
                VPN_CHANNEL_ID,
                "SULTAN BLOCK - Internet Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of device-wide internet blocking service"
                setShowBadge(false)
            }

            val volumeChannel = NotificationChannel(
                VOLUME_CHANNEL_ID,
                "SULTAN BLOCK - Volume Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status and recovery controls for active volume mute lock"
                setShowBadge(false)
            }

            notificationManager?.createNotificationChannel(vpnChannel)
            notificationManager?.createNotificationChannel(volumeChannel)
        }
    }
}
