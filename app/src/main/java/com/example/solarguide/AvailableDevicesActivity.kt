package com.example.solarguide

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.solarguide.databinding.ActivityAvailableDevicesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AvailableDevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvailableDevicesBinding
    private lateinit var database1: DatabaseReference
    private lateinit var database2: DatabaseReference
    private lateinit var popPlayer: MediaPlayer
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    private val criticalBatteryThreshold = 20 // Set the critical battery percentage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvailableDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize MediaPlayer
        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firebase Database (Realtime Database)
        val firebaseRTDB = "https://solarguide-default-rtdb.asia-southeast1.firebasedatabase.app/"
        database1 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("sensorData")
        database2 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("relayData")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("DeviceStatePrefs", Context.MODE_PRIVATE)

        // Fetch and display battery data
        fetchAndDisplayData()

        // Monitor relay data
        monitorRelayData()

        // Setup button actions
        setupButtonActions()

        // Setup menu buttons
        setupMenuButtons()

        // Play startup sound
        popPlayer.start()
    }

    private fun monitorRelayData() {
        database2.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val deviceSocket1 = snapshot.child("deviceSocket1").getValue(Boolean::class.java) ?: false
                val isCharging1 = snapshot.child("isCharging1").getValue(Boolean::class.java) ?: false
                updateUI(deviceSocket1, isCharging1, binding.deviceSocket1Value, binding.socket1StopBtn)

                val deviceSocket2 = snapshot.child("deviceSocket2").getValue(Boolean::class.java) ?: false
                val isCharging2 = snapshot.child("isCharging2").getValue(Boolean::class.java) ?: false
                updateUI(deviceSocket2, isCharging2, binding.deviceSocket2Value, binding.socket2StopBtn)

                val deviceSocket3 = snapshot.child("deviceSocket3").getValue(Boolean::class.java) ?: false
                val isCharging3 = snapshot.child("isCharging3").getValue(Boolean::class.java) ?: false
                updateUI(deviceSocket3, isCharging3, binding.deviceSocket3Value, binding.socket3StopBtn)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AvailableDevicesActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateUI(
        deviceSocket: Boolean,
        isCharging: Boolean,
        deviceSocketTextView: TextView,
        stopButton: View
    ) {
        if (deviceSocket && isCharging) {
            deviceSocketTextView.text = "Charging"
            deviceSocketTextView.setTextColor(getColor(android.R.color.holo_green_dark)) // Set text color to green
            stopButton.visibility = View.VISIBLE
        } else {
            deviceSocketTextView.text = "Not Charging"
            deviceSocketTextView.setTextColor(getColor(android.R.color.holo_red_dark)) // Set text color to red
            stopButton.visibility = View.INVISIBLE
        }

        // Save state in SharedPreferences
        sharedPreferences.edit().apply {
            putBoolean("${deviceSocketTextView.id}_socket", deviceSocket)
            putBoolean("${deviceSocketTextView.id}_charging", isCharging)
            apply()
        }
    }


    private fun setupButtonActions() {
        binding.socket1StopBtn.setOnClickListener { stopDevice("deviceSocket1", "isCharging1") }
        binding.socket2StopBtn.setOnClickListener { stopDevice("deviceSocket2", "isCharging2") }
        binding.socket3StopBtn.setOnClickListener { stopDevice("deviceSocket3", "isCharging3") }
    }

    private fun stopDevice(deviceSocketKey: String, isChargingKey: String) {
        database2.child(deviceSocketKey).setValue(false)
        database2.child(isChargingKey).setValue(false)

        Toast.makeText(
            this,
            "$deviceSocketKey stopped charging",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun fetchAndDisplayData() {
        database1.child("Battery Percentage").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteryPercentage = snapshot.getValue(String::class.java)?.toFloatOrNull() ?: 0f
                binding.devicesBatt.text = batteryPercentage.toString()

                // Trigger notification if battery is below the critical threshold
                if (batteryPercentage <= criticalBatteryThreshold) {
                    showCriticalBatteryNotification(batteryPercentage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AvailableDevicesActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showCriticalBatteryNotification(batteryPercentage: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "battery_alert_channel"
        val channelName = "Battery Alerts"

        // Create Notification Channel if the Android version supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for critical battery levels"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent for opening this activity on notification tap
        val intent = Intent(this, AvailableDevicesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // First Notification: Battery Low Alert
        val notification1 = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_battery_low)
            .setContentTitle("Devices Available For Current Battery Percentage:")
            .setContentText("Mini Fan, Mini Lamp")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification1)

        // Second Notification: Available Devices Alert
        val notification2 = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_battery_low)
            .setContentTitle("Battery Low!")
            .setContentText("Battery is at $batteryPercentage%. Please charge it.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(2, notification2) // Different notification ID
    }

    private fun setupMenuButtons() {
        val frameLayoutIds = listOf(
            R.id.WeatherFrameButton,
            R.id.BatteryFrameButton,
            R.id.PanelFrameButton,
            R.id.DevicesFrameButton,
            R.id.AccountFrameButton
        )

        val activities = listOf(
            WeatherActivity::class.java,
            BatteryPerformanceActivity::class.java,
            PanelPerformanceActivity::class.java,
            AvailableDevicesActivity::class.java,
            AccountActivity::class.java
        )

        ButtonUtils.setMenuButtonClickListener(this, frameLayoutIds, activities)
    }
}
