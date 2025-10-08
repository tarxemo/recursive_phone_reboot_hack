package com.example.phonerebooter
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import java.util.concurrent.TimeUnit
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.phonerebooter.databinding.ActivityMainBinding
import com.example.phonerebooter.receiver.DeviceAdminReceiver
import com.example.phonerebooter.service.UsageTrackingService
import com.example.phonerebooter.util.Constants
import com.example.phonerebooter.util.PreferenceHelper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceHelper
    private lateinit var deviceAdminReceiver: ComponentName
    private val REQUEST_CODE_ENABLE_ADMIN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceHelper(this)
        deviceAdminReceiver = ComponentName(this, DeviceAdminReceiver::class.java)

        setupUI()
        updateUI()
        startOrStopService()
    }
    
    private fun updateUI() {
        val isActive = isServiceRunning()
        binding.statusText.text = if (isActive) getString(R.string.status_active) else getString(R.string.status_inactive)
        binding.toggleButton.isChecked = isActive
        updateTimeRemaining()
    }
    
    private fun setupUI() {
        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isDeviceAdminActive()) {
                    requestDeviceAdmin()
                    // Don't update UI here, wait for device admin to be enabled
                    binding.toggleButton.isChecked = false
                    return@setOnCheckedChangeListener
                }
            }
            startOrStopService()
        }
        
        binding.resetButton.setOnClickListener {
            showResetConfirmation()
        }
    }
    
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { 
            UsageTrackingService::class.java.name == it.service.className 
        }
    }

    private fun updateTimeRemaining() {
        val currentTime = System.currentTimeMillis()
        val lastResetTime = prefs.lastResetTime
        val timeElapsed = currentTime - lastResetTime
        val timeRemaining = maxOf(0, Constants.SHUTDOWN_TIME_MS - timeElapsed)
        val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
        binding.timeRemainingText.text = getString(R.string.time_remaining_format, hours, minutes)
            
        // Update the progress bar (0-100%)
        val progress = ((Constants.SHUTDOWN_TIME_MS - timeRemaining).toFloat() / Constants.SHUTDOWN_TIME_MS * 100).toInt()
        binding.progressBar.progress = progress
        
        // Enable/disable reset button based on time elapsed
        binding.resetButton.isEnabled = timeElapsed > 0
    }

    private fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isAdminActive(deviceAdminReceiver)
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                getString(R.string.device_admin_explanation))
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    private fun enableService() {
        prefs.isServiceEnabled = true
        prefs.resetTimer()
        
        val serviceIntent = Intent(this, UsageTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        updateUI()
    }

    private fun disableService() {
        prefs.isServiceEnabled = false
        val serviceIntent = Intent(this, UsageTrackingService::class.java)
        stopService(serviceIntent)
        updateUI()
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Timer")
            .setMessage("Are you sure you want to reset the usage timer?")
            .setPositiveButton("Reset") { _, _ ->
                prefs.resetTimer()
                updateUI()
                Toast.makeText(this, "Timer has been reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startOrStopService() {
        if (prefs.isServiceEnabled) {
            if (!isDeviceAdminActive()) {
                prefs.isServiceEnabled = false
                updateUI()
            } else {
                enableService()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == Activity.RESULT_OK) {
                binding.toggleButton.isChecked = true
                enableService()
            } else {
                binding.toggleButton.isChecked = false
                Toast.makeText(this, "Device admin permission is required", Toast.LENGTH_SHORT).show()
            }
        }
        updateUI()
    }

    override fun onResume() {
        try {
            super.onResume()
        } catch (e: Exception) {
            // Ignore Vivo-specific service errors
            if (!e.message?.contains("UserProfilingManager")!!) {
                throw e
            }
        }
        updateUI()
    }
}
