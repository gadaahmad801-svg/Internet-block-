package com.example.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SultanBlockApp
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class SultanBlockVpnService : VpnService() {

    companion object {
        const val TAG = "SultanBlockVpnService"
        const val ACTION_START = "com.example.vpn.ACTION_START"
        const val ACTION_STOP = "com.example.vpn.ACTION_STOP"
        private const val NOTIFICATION_ID = 9110
        private const val VPN_MTU = 1500
        private const val SINKHOLE_IPV4 = "10.254.1.2"
        private const val SINKHOLE_IPV6 = "fd00:abcd:ef01::2"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
            }
            ACTION_START -> {
                startVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (isRunning.get()) return

        try {
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

            val builder = Builder().apply {
                setSession("SULTAN BLOCK - Internet Filter")
                setMtu(VPN_MTU)
                // Intercept all IPv4 traffic
                addAddress(SINKHOLE_IPV4, 32)
                addRoute("0.0.0.0", 0)

                // Intercept all IPv6 traffic
                try {
                    addAddress(SINKHOLE_IPV6, 128)
                    addRoute("::", 0)
                } catch (e: Exception) {
                    Log.w(TAG, "IPv6 route configuration warning: ${e.message}")
                }

                // Divert DNS to loopback dummy address to sinkhole all domain queries
                try {
                    addDnsServer(SINKHOLE_IPV4)
                } catch (e: Exception) {
                    Log.w(TAG, "DNS configuration warning: ${e.message}")
                }

                setBlocking(true)
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface. System returned null.")
                VpnStateManager.setError("Failed to initialize VPN interface. Another VPN might be active.")
                stopSelf()
                return
            }

            isRunning.set(true)
            VpnStateManager.updateActive(true)

            // Start blackhole drainage worker thread
            startPacketDrainWorker()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}", e)
            VpnStateManager.setError(e.localizedMessage ?: "Unknown VPN activation error")
            stopVpn()
            stopSelf()
        }
    }

    private fun startPacketDrainWorker() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)

        workerThread = Thread({
            val packetBuffer = ByteBuffer.allocate(32768)
            val byteArray = ByteArray(32768)

            Log.i(TAG, "SULTAN BLOCK blackhole loopback active. Intercepting and sinking all network packets.")

            while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                try {
                    val length = inputStream.read(byteArray)
                    if (length > 0) {
                        // Ingest and drop packet immediately. This prevents any traffic from routing to internet.
                        VpnStateManager.incrementBlocked(1, length.toLong())
                    } else if (length < 0) {
                        break
                    }
                } catch (e: IOException) {
                    if (isRunning.get()) {
                        Log.d(TAG, "VPN read socket closed: ${e.message}")
                    }
                    break
                } catch (t: Throwable) {
                    Log.e(TAG, "Unexpected error in packet sink: ${t.message}")
                    break
                }
            }
            Log.i(TAG, "SULTAN BLOCK packet drain worker stopped.")
        }, "SultanBlockPacketDrain").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    private fun stopVpn() {
        if (!isRunning.getAndSet(false)) return

        try {
            workerThread?.interrupt()
            workerThread = null

            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        } finally {
            VpnStateManager.updateActive(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by system or another VPN app took precedence.")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

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

        val stopIntent = Intent(this, SultanBlockVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SultanBlockApp.VPN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("INTERNET BLOCKED - SULTAN BLOCK")
            .setContentText("All device internet traffic (IPv4, IPv6, DNS) is locked and blackholed.")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "UNBLOCK INTERNET",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
