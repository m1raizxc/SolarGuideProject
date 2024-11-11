package com.example.solarguide

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.solarguide.databinding.ActivityPanelPerformanceBinding
import java.util.*

class PanelPerformanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPerformanceBinding
    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private var databaseListener: ValueEventListener? = null
    private lateinit var popPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)
        firebaseAuth = FirebaseAuth.getInstance()

        // Set up Firebase Realtime Database reference
        val databaseUrl = "https://solarguide-default-rtdb.asia-southeast1.firebasedatabase.app/"
        database = FirebaseDatabase.getInstance(databaseUrl).reference.child("sensorData")

        fetchDataAndDisplay()
        scheduleMidnightReset()
        setupTooltips()
        setupMenuButtons()
    }

    private fun setupTooltips() {
        val usageInfo: ImageView = findViewById(R.id.usageInfo)
        setTooltip(usageInfo, "This icon represents your solar energy consumption from the battery")

        val todayUsage: ImageView = findViewById(R.id.todayUsage_info)
        setTooltip(todayUsage, "Tracks today's solar energy you consume from the battery")

        val totalUsage: ImageView = findViewById(R.id.totalUsageInfo)
        setTooltip(totalUsage, "Tracks all the solar energy you consume from the battery")

        val yieldInfo: ImageView = findViewById(R.id.yieldInfo)
        setTooltip(yieldInfo, "This icon represents solar energy acquired from sunlight")

        val todayYield: ImageView = findViewById(R.id.todayYield_info)
        setTooltip(todayYield, "Tracks today's solar energy acquired from sunlight")

        val totalYield: ImageView = findViewById(R.id.totalYield_Info)
        setTooltip(totalYield, "Tracks all the solar energy acquired from sunlight")
    }

    private fun setTooltip(view: ImageView, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.tooltipText = message
        } else {
            view.setOnLongClickListener {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    private fun fetchDataAndDisplay() {
        databaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val batteryDischargeToday = snapshot.child("Battery Discharge Today").getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val batteryDischargeTotal = snapshot.child("Battery Discharge Total").getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val energyYieldToday = snapshot.child("Energy Yield Today").getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val energyYieldTotal = snapshot.child("Energy Yield Total").getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val isCharging = snapshot.child("isCharging").getValue(Boolean::class.java) ?: false

                    // Update UI
                    binding.TextTodayUsageValue.text = batteryDischargeToday.toString()
                    binding.TextTotalUsageValue.text = batteryDischargeTotal.toString()
                    binding.TextTodayYieldValue.text = energyYieldToday.toString()
                    binding.TextTotalYieldValue.text = energyYieldTotal.toString()
                    updateBatteryIcon(isCharging)

                    Log.d("PanelPerformanceActivity", "Battery Discharge Today: $batteryDischargeToday")
                    Log.d("PanelPerformanceActivity", "Battery Discharge Total: $batteryDischargeTotal")
                    Log.d("PanelPerformanceActivity", "Energy Yield Today: $energyYieldToday")
                    Log.d("PanelPerformanceActivity", "Energy Yield Total: $energyYieldTotal")
                } else {
                    Log.d("PanelPerformanceActivity", "Data snapshot does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PanelPerformanceActivity", "Database error: ${error.message}")
                Toast.makeText(this@PanelPerformanceActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(databaseListener!!)
    }

    private fun updateBatteryIcon(isCharging: Boolean) {
        val batteryIcon: ImageView = binding.batteryIcon
        if (isCharging) {
            Glide.with(this).asGif().load(R.drawable.yield_gif).into(batteryIcon)
        } else {
            Glide.with(this).load(R.drawable.yield).into(batteryIcon)
        }
    }

    private fun scheduleMidnightReset() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MidnightResetReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseListener?.let { database.removeEventListener(it) }
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupMenuButtons() {
        // Set up menu button listeners
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
        popPlayer.start()
    }
}
