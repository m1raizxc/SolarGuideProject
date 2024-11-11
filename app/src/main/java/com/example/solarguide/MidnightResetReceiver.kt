package com.example.solarguide

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Get reference to the current user's document
        val userDoc = firestore.collection("usageData").document(firebaseAuth.currentUser!!.uid)

        // Fetch the current daily and total values
        userDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val todayUsage = document.getDouble("Text_TodayUsageValue")?.toFloat() ?: 0f
                val totalUsage = document.getDouble("Text_TotalUsageValue")?.toFloat() ?: 0f
                val todayYield = document.getDouble("Text_TodayYieldValue")?.toFloat() ?: 0f
                val totalYield = document.getDouble("Text_TotalYieldValue")?.toFloat() ?: 0f

                // Calculate the new total values
                val updatedTotalUsage = totalUsage + todayUsage
                val updatedTotalYield = totalYield + todayYield

                // Update Firestore with the new total values and reset today's values
                userDoc.update(
                    mapOf(
                        "Text_TotalUsageValue" to updatedTotalUsage,
                        "Text_TotalYieldValue" to updatedTotalYield,
                        "Text_TodayUsageValue" to 0f,  // Reset today values
                        "Text_TodayYieldValue" to 0f   // Reset today values
                    )
                ).addOnSuccessListener {
                    Log.d("MidnightResetReceiver", "Daily values reset and total values updated successfully.")
                }.addOnFailureListener {
                    Log.e("MidnightResetReceiver", "Failed to update values: ${it.message}")
                }
            }
        }.addOnFailureListener {
            Log.e("MidnightResetReceiver", "Failed to fetch document: ${it.message}")
        }
    }
}