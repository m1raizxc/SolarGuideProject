package com.example.solarguide

import android.app.Dialog
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solarguide.databinding.ActivityAvailableDevicesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class AvailableDevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvailableDevicesBinding
    private lateinit var database1: DatabaseReference
    private lateinit var database2: DatabaseReference
    private lateinit var popPlayer: MediaPlayer
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_KEY = "DevicePreferences"
        const val ACTIVE_DEVICE_1 = "ActiveDevice1"
        const val ACTIVE_DEVICE_2 = "ActiveDevice2"
        const val ACTIVE_DEVICE_3 = "ActiveDevice3"
        const val MINI_FAN = "MiniFan"
        const val POWER_BANK = "PowerBank"
        const val SMART_PHONE = "SmartPhone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvailableDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_KEY, MODE_PRIVATE)

        // Initialize MediaPlayer
        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firebase Database (Realtime Database)
        val firebaseRTDB = "https://solarguide-default-rtdb.asia-southeast1.firebasedatabase.app/"
        database1 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("sensorData")
        database2 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("relayData")

        // Fetch and display battery data
        fetchAndDisplayData()

        // Setup menu buttons
        setupMenuButtons()

        // Load saved device states
        loadDeviceStates()

        // Set up click listener for addDeviceButton
        binding.addDeviceButton.setOnClickListener {
            showDevicePopup()
        }

        // Remove button listeners for each device layout
        binding.layout1RemoveBtn.setOnClickListener {
            deactivateDevice(ACTIVE_DEVICE_1, "Device in Slot 1 deactivated!", binding.deviceLayout1, binding.layoutAvaible1)
        }

        binding.layout2RemoveBtn.setOnClickListener {
            deactivateDevice(ACTIVE_DEVICE_2, "Device in Slot 2 deactivated!", binding.deviceLayout2, binding.layoutAvailable2)
        }

        binding.layout3RemoveBtn.setOnClickListener {
            deactivateDevice(ACTIVE_DEVICE_3, "Device in Slot 3 deactivated!", binding.deviceLayout3, binding.layoutAvailable3)
        }

        // Play startup sound
        popPlayer.start()
    }

    private fun showDevicePopup() {
        val dialog = Dialog(this)
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.device_popup, binding.root, false)
        dialog.setContentView(dialogView)

        val deviceListView = dialogView.findViewById<ListView>(R.id.deviceListView)
        val closePopupButton = dialogView.findViewById<Button>(R.id.closePopupButton)

        val devices = listOf("Mini Fan", "Power Bank", "Smart Phone")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> activateDevice(MINI_FAN, "Mini Fan activated!", R.drawable.fan, "Mini Fan")
                1 -> activateDevice(POWER_BANK, "Power Bank activated!", R.drawable.powerbank, "Power Bank")
                2 -> activateDevice(SMART_PHONE, "Smart Phone activated!", R.drawable.smartphone, "Smart Phone")
            }
            dialog.dismiss()
        }

        closePopupButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun activateDevice(key: String, message: String, drawableId: Int, deviceName: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        when {
            binding.deviceLayout1.visibility == View.GONE -> setupDeviceSlot(drawableId, deviceName, binding.deviceLayout1, binding.layoutAvaible1, ACTIVE_DEVICE_1, key)
            binding.deviceLayout2.visibility == View.GONE -> setupDeviceSlot(drawableId, deviceName, binding.deviceLayout2, binding.layoutAvailable2, ACTIVE_DEVICE_2, key)
            binding.deviceLayout3.visibility == View.GONE -> setupDeviceSlot(drawableId, deviceName, binding.deviceLayout3, binding.layoutAvailable3, ACTIVE_DEVICE_3, key)
            else -> Toast.makeText(this, "All device slots are occupied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDeviceSlot(drawableId: Int, deviceName: String, deviceLayout: View, availableLayout: View, layoutKey: String, key: String) {
        deviceLayout.visibility = View.VISIBLE
        availableLayout.visibility = View.GONE
        when (deviceLayout) {
            binding.deviceLayout1 -> {
                binding.device1Icon.setImageResource(drawableId)
                binding.textDevice1.text = deviceName
            }
            binding.deviceLayout2 -> {
                binding.device2Icon.setImageResource(drawableId)
                binding.textDevice2.text = deviceName
            }
            binding.deviceLayout3 -> {
                binding.device3Icon.setImageResource(drawableId)
                binding.textDevice3.text = deviceName
            }
        }
        saveDeviceState(key, true, layoutKey)
    }

    private fun deactivateDevice(layoutKey: String, message: String, deviceLayout: View, availableLayout: View) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        deviceLayout.visibility = View.GONE
        availableLayout.visibility = View.VISIBLE
        saveDeviceState("", false, layoutKey)
    }

    private fun saveDeviceState(key: String, isActive: Boolean, layoutKey: String = "") {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, isActive)

        when (layoutKey) {
            ACTIVE_DEVICE_1 -> database2.child("deviceSocket1").setValue(isActive)
            ACTIVE_DEVICE_2 -> database2.child("deviceSocket2").setValue(isActive)
            ACTIVE_DEVICE_3 -> database2.child("deviceSocket3").setValue(isActive)
        }

        if (isActive) editor.putString(layoutKey, key) else editor.remove(layoutKey)
        editor.apply()
    }

    private fun loadDeviceStates() {
        val isMiniFanActive = sharedPreferences.getBoolean(MINI_FAN, false)
        val isPowerBankActive = sharedPreferences.getBoolean(POWER_BANK, false)
        val isSmartPhoneActive = sharedPreferences.getBoolean(SMART_PHONE, false)

        setupDeviceState(isMiniFanActive, binding.deviceLayout1, binding.layoutAvaible1)
        setupDeviceState(isPowerBankActive, binding.deviceLayout2, binding.layoutAvailable2)
        setupDeviceState(isSmartPhoneActive, binding.deviceLayout3, binding.layoutAvailable3)
    }

    private fun setupDeviceState(isActive: Boolean, deviceLayout: View, availableLayout: View) {
        deviceLayout.visibility = if (isActive) View.VISIBLE else View.GONE
        availableLayout.visibility = if (isActive) View.GONE else View.VISIBLE
    }

    private fun fetchAndDisplayData() {
        database1.child("Battery Percentage").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteryPercentage = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0
                binding.devicesBatt.text = String.format(Locale.getDefault(), "%d%%", batteryPercentage)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AvailableDevicesActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

    override fun onDestroy() {
        super.onDestroy()
        if (::popPlayer.isInitialized) popPlayer.release()
    }
}
