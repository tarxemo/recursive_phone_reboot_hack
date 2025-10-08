package com.example.phonerebooter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.phonerebooter.MainActivity
import com.example.phonerebooter.R
import com.example.phonerebooter.receiver.DeviceAdminReceiver
import com.example.phonerebooter.util.Constants
import com.example.phonerebooter.util.PreferenceHelper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class UsageTrackingService : Service() {
    private lateinit var executor: java.util.concurrent.ScheduledExecutorService
    private var scheduledFuture: ScheduledFuture<*>? = null
    private lateinit var prefs: PreferenceHelper
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "UsageTrackingServiceChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceHelper(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        executor = Executors.newSingleThreadScheduledExecutor()
        
        scheduledFuture = executor.scheduleAtFixedRate({
            val currentTime = System.currentTimeMillis()
            val lastResetTime = prefs.lastResetTime
            
            if (currentTime - lastResetTime >= Constants.SHUTDOWN_TIME_MS) {
                // Time's up! Shutdown the device
                shutdownDevice()
            }
        }, 0, 1, TimeUnit.MINUTES) // Check every minute
    }

    private fun shutdownDevice() {
        try {
            // Log the shutdown attempt
            android.util.Log.d("UsageTrackingService", "Time's up! Showing lock screen...")
            
            // Show the lock screen activity
            LockScreenActivity.start(this)
            
            // Try to lock the device using device admin
            try {
                val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.lockNow()
                    android.util.Log.d("UsageTrackingService", "Device locked successfully")
                } else {
                    android.util.Log.d("UsageTrackingService", "Cannot lock device: admin not active")
                }
            } catch (e: Exception) {
                android.util.Log.e("UsageTrackingService", "Error locking device", e)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("UsageTrackingService", "Unexpected error in shutdownDevice", e)
        } finally {
            // Always stop the service to prevent it from running indefinitely
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Usage Tracking Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phone Usage Tracker")
            .setContentText("Monitoring phone usage time")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledFuture?.cancel(true)
        executor.shutdown()
    }
}
