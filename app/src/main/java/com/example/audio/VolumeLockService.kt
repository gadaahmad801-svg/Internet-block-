package com.example.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SultanBlockApp

class VolumeLockService : Service() {

    companion object {
        const val TAG = "VolumeLockService"
        const val ACTION_START = "com.example.audio.ACTION_START"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"
        private const val NOTIFICATION_ID = 9111

        val MONITORED_STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.STREAM_ALARM
        )
    }

    private lateinit var audioManager: AudioManager
    private var volumeObserver: ContentObserver? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isClamping = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopLock()
                stopSelf()
            }
            ACTION_START -> {
                startLock()
            }
        }
        return START_NOT_STICKY
    }

    private fun startLock() {
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

        VolumeStateManager.updateLockActive(true)

        // Force silence right away
        enforceVolumeClamp()

        // Register ContentObserver for Settings.System volume mutations
        registerVolumeObserver()

        // Register Volume Change broadcast receiver
        registerVolumeReceiver()

        // Request Audio Focus to preempt and mute background playback
        requestExclusiveAudioFocus()
    }

    private fun registerVolumeObserver() {
        if (volumeObserver != null) return

        volumeObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (!isClamping) {
                    enforceVolumeClamp()
                }
            }
        }

        try {
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not register ContentObserver on Settings.System: ${e.message}")
        }
    }

    private fun registerVolumeReceiver() {
        if (volumeReceiver != null) return

        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    enforceVolumeClamp()
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            registerReceiver(volumeReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register VOLUME_CHANGED_ACTION receiver: ${e.message}")
        }
    }

    private fun enforceVolumeClamp() {
        isClamping = true
        var suppressedAny = false

        try {
            val mediaOnly = VolumeStateManager.lockMediaOnly.value
            val targetStreams = if (mediaOnly) {
                intArrayOf(AudioManager.STREAM_MUSIC)
            } else {
                MONITORED_STREAMS
            }

            for (stream in targetStreams) {
                val current = audioManager.getStreamVolume(stream)
                if (current > 0) {
                    suppressedAny = true
                    try {
                        audioManager.setStreamVolume(stream, 0, 0)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Permission needed to adjust stream $stream: ${e.message}")
                    }
                }
            }

            // Also mute stream music via adjustStreamVolume
            try {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } catch (e: Exception) {
                // Ignore if not supported on this OEM
            }

            if (suppressedAny) {
                VolumeStateManager.incrementSuppression()
            }

            updateStreamSnapshot()
        } catch (e: Exception) {
            Log.e(TAG, "Error during volume clamp enforcement: ${e.message}")
        } finally {
            isClamping = false
        }
    }

    private fun updateStreamSnapshot() {
        val streamInfos = mutableListOf<AudioStreamInfo>()
        val streamNames = mapOf(
            AudioManager.STREAM_MUSIC to "Media & Apps",
            AudioManager.STREAM_RING to "Ringtone",
            AudioManager.STREAM_NOTIFICATION to "Notifications",
            AudioManager.STREAM_SYSTEM to "System Sounds",
            AudioManager.STREAM_VOICE_CALL to "Voice Call",
            AudioManager.STREAM_ALARM to "Alarm"
        )

        for (stream in MONITORED_STREAMS) {
            try {
                val current = audioManager.getStreamVolume(stream)
                val max = audioManager.getStreamMaxVolume(stream)
                val name = streamNames[stream] ?: "Stream $stream"
                streamInfos.add(
                    AudioStreamInfo(
                        streamType = stream,
                        name = name,
                        currentVolume = current,
                        maxVolume = max,
                        isLocked = current == 0
                    )
                )
            } catch (e: Exception) {
                // Ignore inaccessible streams
            }
        }
        VolumeStateManager.updateStreams(streamInfos)
    }

    private fun requestExclusiveAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                            enforceVolumeClamp()
                        }
                    }
                    .build()

                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    { enforceVolumeClamp() },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request warning: ${e.message}")
        }
    }

    private fun stopLock() {
        try {
            volumeObserver?.let {
                contentResolver.unregisterContentObserver(it)
                volumeObserver = null
            }
            volumeReceiver?.let {
                unregisterReceiver(it)
                volumeReceiver = null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                audioFocusRequest = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering volume monitors: ${e.message}")
        } finally {
            VolumeStateManager.updateLockActive(false)
            updateStreamSnapshot()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onDestroy() {
        stopLock()
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

        val stopIntent = Intent(this, VolumeLockService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SultanBlockApp.VOLUME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("VOLUME LOCKED - SULTAN BLOCK")
            .setContentText("All audio streams are clamped to 0. Background suppression active.")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DISABLE VOLUME LOCK",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
