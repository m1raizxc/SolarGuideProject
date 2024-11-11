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

class AvailableDevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvailableDevicesBinding
    private lateinit var database1: DatabaseReference
    private lateinit var database2: DatabaseReference
    private lateinit var popPlayer: MediaPlayer
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvailableDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("DevicePreferences", MODE_PRIVATE)

        // Initialize MediaPlayer
        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firebase Database (Realtime Database)
        val firebaseRTDB = "https://solarguide-default-rtdb.asia-southeast1.firebasedatabase.app/"
        database1 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("sensorData")
        database2 = FirebaseDatabase.getInstance(firebaseRTDB).getReference("relayData")

        // Fetch and display battery data from database1
        fetchAndDisplayData()

        // Setup menu buttons
        setupMenuButtons()

        // Load saved device states
        loadDeviceStates()

        // Set up click listener for addDeviceButton
        val addDeviceButton = binding.addDeviceButton
        addDeviceButton.setOnClickListener {
            showDevicePopup()
        }

        // Remove button listeners for each device layout
        binding.layout1RemoveBtn.setOnClickListener {
            if (binding.deviceLayout1.visibility == View.VISIBLE) {
                deactivateDevice(
                    layoutKey = "ActiveDevice1",
                    message = "Device in Slot 1 deactivated!",
                    deviceLayout = binding.deviceLayout1,
                    availableLayout = binding.layoutAvaible1
                )
            }
        }

        binding.layout2RemoveBtn.setOnClickListener {
            if (binding.deviceLayout2.visibility == View.VISIBLE) {
                deactivateDevice(
                    layoutKey = "ActiveDevice2",
                    message = "Device in Slot 2 deactivated!",
                    deviceLayout = binding.deviceLayout2,
                    availableLayout = binding.layoutAvailable2
                )
            }
        }

        binding.layout3RemoveBtn.setOnClickListener {
            if (binding.deviceLayout3.visibility == View.VISIBLE) {
                deactivateDevice(
                    layoutKey = "ActiveDevice3",
                    message = "Device in Slot 3 deactivated!",
                    deviceLayout = binding.deviceLayout3,
                    availableLayout = binding.layoutAvailable3
                )
            }
        }


        popPlayer.start()
    }

    // Function to show the device popup
    private fun showDevicePopup() {
        val dialog = Dialog(this)

        // Inflate layout with the root dialog view
        val dialogView: View = LayoutInflater.from(this)
            .inflate(R.layout.device_popup, dialog.findViewById(android.R.id.content), false)
        dialog.setContentView(dialogView)

        // Initialize views in the dialog
        val deviceListView = dialogView.findViewById<ListView>(R.id.deviceListView)
        val closePopupButton = dialogView.findViewById<Button>(R.id.closePopupButton)

        val devices = listOf("", "", "")

        // Set up an adapter for the ListView to display devices
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        deviceListView.adapter = adapter

        // Handle list item click
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    // Mini Fan selected
                    activateDevice(
                        key = "MiniFan",
                        message = "Mini Fan activated!",
                        drawableId = R.drawable.fan,    // Replace with actual drawable resource
                        deviceName = "Mini Fan"
                    )
                }
                1 -> {
                    // Power Bank selected
                    activateDevice(
                        key = "PowerBank",
                        message = "Power Bank activated!",
                        drawableId = R.drawable.powerbank,  // Replace with actual drawable resource
                        deviceName = "Power Bank"
                    )
                }
                2 -> {
                    // Smart Phone selected
                    activateDevice(
                        key = "SmartPhone",
                        message = "Smart Phone activated!",
                        drawableId = R.drawable.smartphone, // Replace with actual drawable resource
                        deviceName = "Smart Phone"
                    )
                }
            }
            dialog.dismiss() // Close the dialog after selecting an item
        }

        // Handle close button click
        closePopupButton.setOnClickListener {
            dialog.dismiss() // Close the dialog
        }

        dialog.show()
    }

    // Function to activate a device and update its layout, icon, and text dynamically
    private fun activateDevice(
        key: String,
        message: String,
        drawableId: Int,
        deviceName: String
    ) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        when {
            binding.deviceLayout1.visibility == View.GONE -> {
                binding.deviceLayout1.visibility = View.VISIBLE
                binding.layoutAvaible1.visibility = View.GONE
                binding.device1Icon.setImageResource(drawableId)
                binding.textDevice1.text = deviceName
                saveDeviceState(key, true, "ActiveDevice1")
            }
            binding.deviceLayout2.visibility == View.GONE -> {
                binding.deviceLayout2.visibility = View.VISIBLE
                binding.layoutAvailable2.visibility = View.GONE
                binding.device2Icon.setImageResource(drawableId)
                binding.textDevice2.text = deviceName
                saveDeviceState(key, true, "ActiveDevice2")
            }
            binding.deviceLayout3.visibility == View.GONE -> {
                binding.deviceLayout3.visibility = View.VISIBLE
                binding.layoutAvailable3.visibility = View.GONE
                binding.device3Icon.setImageResource(drawableId)
                binding.textDevice3.text = deviceName
                saveDeviceState(key, true, "ActiveDevice3")
            }
            else -> {
                Toast.makeText(this, "All device slots are occupied!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Function to deactivate a device and save its state
    private fun deactivateDevice(layoutKey: String, message: String, deviceLayout: View, availableLayout: View) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        deviceLayout.visibility = View.GONE
        availableLayout.visibility = View.VISIBLE

        // Clear the active device in SharedPreferences and update Firebase
        val editor = sharedPreferences.edit()
        editor.remove(layoutKey) // Remove the active device from the specific slot
        editor.apply()

        // Update the corresponding relayData slot in Firebase based on layoutKey
        when (layoutKey) {
            "ActiveDevice1" -> database2.child("deviceSocket1").setValue(false)
            "ActiveDevice2" -> database2.child("deviceSocket2").setValue(false)
            "ActiveDevice3" -> database2.child("deviceSocket3").setValue(false)
        }
    }






    // Function to save the device's active state and update the Realtime Database
    private fun saveDeviceState(key: String, isActive: Boolean, layoutKey: String = "") {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, isActive)

        when (layoutKey) {
            "ActiveDevice1" -> database2.child("deviceSocket1").setValue(isActive)
            "ActiveDevice2" -> database2.child("deviceSocket2").setValue(isActive)
            "ActiveDevice3" -> database2.child("deviceSocket3").setValue(isActive)
        }

        if (isActive) {
            editor.putString(layoutKey, key)
        } else {
            editor.remove(layoutKey)
        }
        editor.apply()
    }


    // Function to load the device's active states
    private fun loadDeviceStates() {
        val isMiniFanActive = sharedPreferences.getBoolean("MiniFan", false)
        val isPowerBankActive = sharedPreferences.getBoolean("PowerBank", false)
        val isSmartPhoneActive = sharedPreferences.getBoolean("SmartPhone", false)

        if (isMiniFanActive) {
            binding.deviceLayout1.visibility = View.VISIBLE
            binding.layoutAvaible1.visibility = View.GONE
        } else {
            binding.deviceLayout1.visibility = View.GONE
            binding.layoutAvaible1.visibility = View.VISIBLE
        }

        if (isPowerBankActive) {
            binding.deviceLayout2.visibility = View.VISIBLE
            binding.layoutAvailable2.visibility = View.GONE
        } else {
            binding.deviceLayout2.visibility = View.GONE
            binding.layoutAvailable2.visibility = View.VISIBLE
        }

        if (isSmartPhoneActive) {
            binding.deviceLayout3.visibility = View.VISIBLE
            binding.layoutAvailable3.visibility = View.GONE
        } else {
            binding.deviceLayout3.visibility = View.GONE
            binding.layoutAvailable3.visibility = View.VISIBLE
        }
    }

    // Function to fetch and display battery data
    private fun fetchAndDisplayData() {
        database1.child("Battery Percentage").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteryPercentage = snapshot.getValue(String::class.java)?.toIntOrNull() ?: 0
                binding.devicesBatt.text = batteryPercentage.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors here
            }
        })
    }


    // Function to set up menu buttons
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
