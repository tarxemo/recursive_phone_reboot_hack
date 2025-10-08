package com.example.phonerebooter

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.phonerebooter.databinding.ActivityLockScreenBinding
import com.example.phonerebooter.receiver.DeviceAdminReceiver

class LockScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var deviceAdminReceiver: ComponentName
    private lateinit var devicePolicyManager: DevicePolicyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the activity fullscreen and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceAdminReceiver = ComponentName(this, DeviceAdminReceiver::class.java)
        
        // Set the message text
        binding.messageText.text = getString(R.string.lock_screen_message)
        
        // Try to lock the device
        if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            // Add a small delay to ensure the UI is shown before locking
            binding.root.postDelayed({
                devicePolicyManager.lockNow()
            }, 1000)
        }
        
        // Prevent back button from working
        onBackPressedDispatcher.addCallback { /* Do nothing */ }
    }
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}
