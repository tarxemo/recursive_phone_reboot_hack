package com.example.phonerebooter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.phonerebooter.service.UsageTrackingService
import com.example.phonerebooter.util.PreferenceHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val prefs = PreferenceHelper(context)
            if (prefs.isServiceEnabled) {
                val serviceIntent = Intent(context, UsageTrackingService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
