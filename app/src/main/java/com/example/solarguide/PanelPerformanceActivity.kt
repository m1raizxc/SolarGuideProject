package com.example.solarguide

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.example.solarguide.databinding.ActivityPanelPerformanceBinding

class PanelPerformanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPerformanceBinding
    private lateinit var sensorDataRef: DatabaseReference
    private lateinit var dailyRef: DatabaseReference
    private lateinit var cumulativeRef: DatabaseReference
    private var databaseListener: ValueEventListener? = null
    private lateinit var popPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dailyListener: ValueEventListener
    private lateinit var cumulativeListener: ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        // Initialize Firebase Database references
        val mainDbUrl = "https://solarguide-default-rtdb.asia-southeast1.firebasedatabase.app/"
        sensorDataRef = FirebaseDatabase.getInstance(mainDbUrl).reference.child("sensorData")

        val secondaryDbUrl = "https://solarguide-bc8d9.asia-southeast1.firebasedatabase.app/"
        dailyRef = FirebaseDatabase.getInstance(secondaryDbUrl).reference.child("daily")
        cumulativeRef = FirebaseDatabase.getInstance(secondaryDbUrl).reference.child("cumulative")

        // Start listening to realtime updates
        handleRealtimeUpdates()

        // Fetch cumulative data and display it
        fetchCumulativeDataAndDisplay()

        fetchDataAndDisplay()

        startSummingAndResettingDailyValues()

        updateDailyDataFromSensor()

        setupTooltips()
        setupMenuButtons()
    }

    private fun updateDailyDataFromSensor() {
        databaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val newBatteryDischarge = snapshot.child("Battery Discharge Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val newEnergyYield = snapshot.child("Energy Yield Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                    dailyRef.get().addOnSuccessListener { dailySnapshot ->
                        val currentBatteryDischarge = dailySnapshot.child("Battery Discharge Today")
                            .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                        val currentEnergyYield = dailySnapshot.child("Energy Yield Today")
                            .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                        // Add new data to existing daily data
                        val updatedBatteryDischarge = currentBatteryDischarge + newBatteryDischarge
                        val updatedEnergyYield = currentEnergyYield + newEnergyYield

                        // Prepare data for updating
                        val dailyUpdates = mapOf(
                            "Battery Discharge Today" to updatedBatteryDischarge.toString(),
                            "Energy Yield Today" to updatedEnergyYield.toString()
                        )

                        // Log and update dailyRef
                        Log.d("PanelPerformanceActivity", "Updated Daily Data: Discharge=$updatedBatteryDischarge, Yield=$updatedEnergyYield")
                        dailyRef.updateChildren(dailyUpdates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("PanelPerformanceActivity", "Successfully updated dailyRef.")
                            } else {
                                Log.e("PanelPerformanceActivity", "Failed to update dailyRef: ${task.exception}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PanelPerformanceActivity", "Error updating daily data from sensor: ", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PanelPerformanceActivity", "Failed to listen to sensorDataRef updates: ${error.message}")
            }
        }

        // Add the listener to sensorDataRef
        sensorDataRef.addValueEventListener(databaseListener!!)
    }




    private fun fetchDataAndDisplay() {
        dailyRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                try {
                    val batteryDischargeToday = snapshot.child("Battery Discharge Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val energyYieldToday = snapshot.child("Energy Yield Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                    // Update the UI
                    binding.TextTodayUsageValue.text = batteryDischargeToday.toString()
                    binding.TextTodayYieldValue.text = energyYieldToday.toString()

                    Log.d("PanelPerformanceActivity", "Displayed Today Data: Discharge=$batteryDischargeToday, Yield=$energyYieldToday")
                } catch (e: Exception) {
                    Log.e("PanelPerformanceActivity", "Error processing daily snapshot: ", e)
                    Toast.makeText(this, "Error displaying today data", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("PanelPerformanceActivity", "Daily snapshot does not exist")
                Toast.makeText(this, "No today data available", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("PanelPerformanceActivity", "Failed to fetch today data: ", e)
            Toast.makeText(this, "Failed to retrieve today data", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetchCumulativeDataAndDisplay() {
        cumulativeRef.get().addOnSuccessListener { cumulativeSnapshot ->
            try {
                val batteryDischargeTotal = cumulativeSnapshot.child("Battery Discharge Total")
                    .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                val energyYieldTotal = cumulativeSnapshot.child("Energy Yield Total")
                    .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                // Bind cumulative totals to UI
                binding.TextTotalUsageValue.text = batteryDischargeTotal.toString()
                binding.TextTotalYieldValue.text = energyYieldTotal.toString()

                Log.d("PanelPerformanceActivity", "Battery Discharge Total: $batteryDischargeTotal")
                Log.d("PanelPerformanceActivity", "Energy Yield Total: $energyYieldTotal")
            } catch (e: Exception) {
                Log.e("PanelPerformanceActivity", "Error processing cumulative snapshot: ", e)
                Toast.makeText(this@PanelPerformanceActivity, "Error processing cumulative data", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("PanelPerformanceActivity", "Failed to retrieve cumulative data: ", e)
            Toast.makeText(this@PanelPerformanceActivity, "Failed to retrieve cumulative data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSummingAndResettingDailyValues() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                dailyRef.get().addOnSuccessListener { dailySnapshot ->
                    try {
                        val batteryDischargeToday = dailySnapshot.child("Battery Discharge Today")
                            .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                        val energyYieldToday = dailySnapshot.child("Energy Yield Today")
                            .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                        if (batteryDischargeToday > 0 || energyYieldToday > 0) {
                            cumulativeRef.get().addOnSuccessListener { cumulativeSnapshot ->
                                val batteryDischargeTotal = cumulativeSnapshot.child("Battery Discharge Total")
                                    .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                                val energyYieldTotal = cumulativeSnapshot.child("Energy Yield Total")
                                    .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                                val newBatteryDischargeTotal = batteryDischargeTotal + batteryDischargeToday
                                val newEnergyYieldTotal = energyYieldTotal + energyYieldToday

                                // Update cumulative totals
                                val cumulativeUpdates = mapOf(
                                    "Battery Discharge Total" to newBatteryDischargeTotal.toString(),
                                    "Energy Yield Total" to newEnergyYieldTotal.toString()
                                )
                                cumulativeRef.updateChildren(cumulativeUpdates).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("PanelPerformanceActivity", "Cumulative totals updated successfully.")

                                        // Reset daily values to 0 after updating cumulative totals
                                        val dailyReset = mapOf(
                                            "Battery Discharge Today" to "0",
                                            "Energy Yield Today" to "0"
                                        )
                                        dailyRef.updateChildren(dailyReset).addOnCompleteListener { resetTask ->
                                            if (resetTask.isSuccessful) {
                                                Log.d("PanelPerformanceActivity", "Daily values reset to 0.")
                                            } else {
                                                Log.e("PanelPerformanceActivity", "Failed to reset daily values: ${resetTask.exception}")
                                            }
                                        }
                                    } else {
                                        Log.e("PanelPerformanceActivity", "Failed to update cumulative totals: ${task.exception}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PanelPerformanceActivity", "Error processing cumulative update: ", e)
                    }
                }

                // Schedule the next update in 1 minute
                handler.postDelayed(this, 60000)
            }
        }, 60000)
    }




    private fun handleRealtimeUpdates() {
        // Listener for dailyRef (today data)
        dailyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val batteryDischargeToday = snapshot.child("Battery Discharge Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val energyYieldToday = snapshot.child("Energy Yield Today")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                    // Update the UI
                    binding.TextTodayUsageValue.text = batteryDischargeToday.toString()
                    binding.TextTodayYieldValue.text = energyYieldToday.toString()

                    Log.d("PanelPerformanceActivity", "Realtime Today Data: Discharge=$batteryDischargeToday, Yield=$energyYieldToday")
                } catch (e: Exception) {
                    Log.e("PanelPerformanceActivity", "Error updating today data UI: ", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PanelPerformanceActivity", "Failed to listen to dailyRef updates: ${error.message}")
            }
        })

        // Listener for cumulativeRef (total data)
        cumulativeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val batteryDischargeTotal = snapshot.child("Battery Discharge Total")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f
                    val energyYieldTotal = snapshot.child("Energy Yield Total")
                        .getValue(String::class.java)?.toFloatOrNull() ?: 0f

                    // Update the UI
                    binding.TextTotalUsageValue.text = batteryDischargeTotal.toString()
                    binding.TextTotalYieldValue.text = energyYieldTotal.toString()

                    Log.d("PanelPerformanceActivity", "Realtime Cumulative Data: Discharge=$batteryDischargeTotal, Yield=$energyYieldTotal")
                } catch (e: Exception) {
                    Log.e("PanelPerformanceActivity", "Error updating cumulative data UI: ", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PanelPerformanceActivity", "Failed to listen to cumulativeRef updates: ${error.message}")
            }
        })
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorDataRef.removeEventListener(databaseListener ?: return)
            dailyRef.removeEventListener(dailyListener)
            cumulativeRef.removeEventListener(cumulativeListener)
        } catch (e: Exception) {
            Log.e("PanelPerformanceActivity", "Error removing database listeners: ", e)
        }
        popPlayer.release()
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
        popPlayer.start()
    }
}