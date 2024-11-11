package com.example.solarguide

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout

object ButtonUtils {
    private const val PREFS_NAME = "MenuButtonPrefs"
    private const val KEY_SELECTED_BUTTON_ID = "selectedButtonId"

    fun setMenuButtonClickListener(
        activity: Activity,
        frameLayoutIds: List<Int>,
        activities: List<Class<out Activity>>
    ) {
        require(frameLayoutIds.size == activities.size) { "FrameLayout IDs and activities lists must have the same size" }

        val frameLayouts = frameLayoutIds.map { activity.findViewById<FrameLayout>(it) }

        // Restore the selected state
        val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedFrameLayoutId = sharedPreferences.getInt(KEY_SELECTED_BUTTON_ID, -1)
        frameLayouts.forEach { it.isSelected = it.id == selectedFrameLayoutId }

        frameLayouts.forEachIndexed { index, frameLayout ->
            frameLayout.setOnClickListener {
                // Check if the clicked button is already selected
                if (frameLayout.isSelected) {
                    return@setOnClickListener // Prevent further action if already selected
                }

                // Disable all buttons
                frameLayouts.forEach { it.isEnabled = false }
                frameLayouts.forEach { it.isSelected = false }

                // Set the clicked button as selected
                frameLayout.isSelected = true

                // Save the selected button ID
                with(sharedPreferences.edit()) {
                    putInt(KEY_SELECTED_BUTTON_ID, frameLayout.id)
                    apply()
                }

                // Start the respective activity
                val intent = Intent(activity, activities[index])
                activity.startActivity(intent)

                // Optionally, you can use an override animation here
                // activity.overridePendingTransition(R.anim.enter, R.anim.exit)

                // Re-enable buttons after a short delay (optional)
                frameLayout.postDelayed({
                    frameLayouts.forEach { it.isEnabled = true }
                }, 1000) // Adjust delay as needed
            }
        }
    }
}
