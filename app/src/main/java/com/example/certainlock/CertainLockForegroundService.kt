package com.example.certainlock

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SultanBlockApp

class CertainLockForegroundService : Service() {

    companion object {
        const val TAG = "CertainLockFGS"
        const val ACTION_START = "com.example.certainlock.ACTION_START"
        const val ACTION_STOP = "com.example.certainlock.ACTION_STOP"
        private const val NOTIFICATION_ID = 9112
        private const val CHECK_INTERVAL_MS = 600L
    }

    private var usageStatsManager: UsageStatsManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            checkForegroundAppAndRedirect()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }
            ACTION_START -> {
                startMonitoring()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        CertainLockManager.updateActiveState(true)
        isMonitoring = true
        handler.removeCallbacks(monitorRunnable)
        handler.post(monitorRunnable)
        Log.i(TAG, "Certain Lock foreground monitoring service started.")
    }

    private fun checkForegroundAppAndRedirect() {
        // If accessibility service is actively connected, let it handle window events without polling
        if (CertainLockAccessibilityService.isServiceConnected) return

        val usm = usageStatsManager ?: return
        val time = System.currentTimeMillis()
        val events = usm.queryEvents(time - 5000, time) ?: return
        val event = UsageEvents.Event()

        var currentForegroundPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }

        if (currentForegroundPackage != null && CertainLockManager.shouldRedirect(currentForegroundPackage, this)) {
            CertainLockManager.returnToHomeViaIntent(this)
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
        CertainLockManager.updateActiveState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Certain Lock foreground monitoring service stopped.")
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CertainLockForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SultanBlockApp.CERTAIN_LOCK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CERTAIN LOCK ACTIVE")
            .setContentText("Restricted apps are redirected directly to Home Screen upon launch.")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DISARM CERTAIN LOCK",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
